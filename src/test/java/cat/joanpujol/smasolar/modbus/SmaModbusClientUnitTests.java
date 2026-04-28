/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cat.joanpujol.smasolar.modbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ModbusRequest;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadInputRegistersResponse;
import com.digitalpetri.modbus.responses.ModbusResponse;
import io.netty.buffer.Unpooled;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class SmaModbusClientUnitTests {

	@Test
	void shouldReleaseResponseBufferAfterAtomicRead() {
		ModbusRegister<Number> register = new ModbusRegister<>(30001, "TEST_REGISTER", "Test register", ModbusDataType.U16,
				ModbusDataFormat.FIX0, ModbusAccesType.READ_ONLY);
		SmaModbusRequest request = SmaModbusRequest.newBuilder(SmaModbusRequest.Type.READ).unitId(3).addRegister(register)
				.build();

		var registers = Unpooled.buffer().writeShort(123);
		var modbusResponse = new ReadInputRegistersResponse(registers);
		SmaModbusClient client = new TestSmaModbusClient(CompletableFuture.completedFuture(modbusResponse));

		SmaModbusResponse response = client.read(request).block();

		assertThat(response.getRegisterValue(register)).isEqualTo(123);
		assertThat(registers.refCnt()).isZero();
	}

	@Test
	void shouldReleaseResponseBufferWhenRegisterParsingFails() {
		ModbusRegister<String> register = new ModbusRegister<>(30001, "TEST_STRING_REGISTER", "Test string register",
				ModbusDataType.STR32, ModbusDataFormat.UTF8, ModbusAccesType.READ_ONLY);

		var registers = Unpooled.wrappedBuffer(new byte[] { 1, 2, 3 });
		var modbusResponse = new ReadInputRegistersResponse(registers);
		SmaModbusClient client = new TestSmaModbusClient(CompletableFuture.completedFuture(modbusResponse));

		assertThatThrownBy(() -> client.readRegister(register, 7).block()).isInstanceOf(IndexOutOfBoundsException.class);
		assertThat(registers.refCnt()).isZero();
	}

	private static final class TestSmaModbusClient extends SmaModbusClient {
		private final ModbusTcpMaster master;

		private TestSmaModbusClient(CompletableFuture<? extends ModbusResponse> responseFuture) {
			super("127.0.0.1", 502, registerReader -> 3);
			this.master = new StubModbusTcpMaster(responseFuture);
		}

		@Override
		protected ModbusTcpMaster createModbusClient() {
			return master;
		}
	}

	private static final class StubModbusTcpMaster extends ModbusTcpMaster {
		private final CompletableFuture<? extends ModbusResponse> responseFuture;

		private StubModbusTcpMaster(CompletableFuture<? extends ModbusResponse> responseFuture) {
			super(new ModbusTcpMasterConfig.Builder("127.0.0.1").build());
			this.responseFuture = responseFuture;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T extends ModbusResponse> CompletableFuture<T> sendRequest(ModbusRequest request, int unitId) {
			assertThat(request).isInstanceOf(ReadInputRegistersRequest.class);
			return (CompletableFuture<T>) responseFuture;
		}
	}
}
