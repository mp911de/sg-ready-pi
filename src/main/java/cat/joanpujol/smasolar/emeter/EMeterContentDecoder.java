package cat.joanpujol.smasolar.emeter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Decoder for EMeter messages */
public class EMeterContentDecoder extends MessageToMessageDecoder<DatagramPacket> {
  private static final int TYPE_COUNTER = 8;
  private static final int TYPE_MEASURE = 4;

  private static final Logger logger = LoggerFactory.getLogger(EMeterContentDecoder.class);

  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out)
      throws Exception {
    var content = msg.content();

    EMeterLecture lecture = new EMeterLecture();

    if (logger.isTraceEnabled())
      logger.trace("Received message {}", ByteBufUtil.prettyHexDump(content));

    // 0
    var smaHeader = content.readSlice(12);
    // 12
    var dataLength = content.readUnsignedShort();
    // 16
    content.skipBytes(4);
    // 18
    var susy = content.readUnsignedShort();
    // 20
    var serno = content.readUnsignedInt();
    var serial = String.format("%05d%010d", susy, serno);
    lecture.setDeviceServiceNumber(serial);
    // 24
    lecture.setTicker(content.readUnsignedInt());
    // 28
    var channelsBuffer = content.slice();

    // Parse all measures in channel data
    while (channelsBuffer.isReadable()) {
      parseMeasure(channelsBuffer, lecture);
    }

    out.add(lecture);
  }

  private void parseMeasure(ByteBuf obis, EMeterLecture reading) {
    var channel = obis.readUnsignedByte();
    var idx = obis.readUnsignedByte();
    var typ = obis.readUnsignedByte();
    var tariff = obis.readUnsignedByte();

    long val;
    if (typ == TYPE_COUNTER) {
      var val1 = obis.readUnsignedInt();
      var val2 = obis.readUnsignedInt();
      val = val1 * 65536 + val2;
      fillValue(val, idx, typ, reading);
    } else if (typ == TYPE_MEASURE) {
      val = obis.readUnsignedInt();
      fillValue(val, idx, typ, reading);
    }
  }

  private void fillValue(long val, short idx, short typ, EMeterLecture reading) {
    if (typ == TYPE_COUNTER) {
      switch (idx) {
        case 1:
          reading.getCounterSum().setActivePower(val / 3600000d);
          break;
        case 2:
          reading.getCounterSum().setNegativeActivePower(val / 3600000d);
          break;
        case 3:
          reading.getCounterSum().setReactivePower(val / 3600000d);
          break;
        case 4:
          reading.getCounterSum().setNegativeReactivePower(val / 3600000d);
          break;
        case 9:
          reading.getCounterSum().setApparentPower(val / 3600000d);
          break;
        case 10:
          reading.getCounterSum().setNegativeApparentPower(val / 3600000d);
          break;
        case 21:
          reading.getCounterPhase1().setActivePower(val / 3600000d);
          break;
        case 22:
          reading.getCounterPhase1().setNegativeActivePower(val / 3600000d);
          break;
        case 23:
          reading.getCounterPhase1().setReactivePower(val / 3600000d);
          break;
        case 24:
          reading.getCounterPhase1().setNegativeReactivePower(val / 3600000d);
          break;
        case 29:
          reading.getCounterPhase1().setApparentPower(val / 3600000d);
          break;
        case 30:
          reading.getCounterPhase1().setNegativeApparentPower(val / 3600000d);
          break;
        case 41:
          reading.getCounterPhase2().setActivePower(val / 3600000d);
          break;
        case 42:
          reading.getCounterPhase2().setNegativeActivePower(val / 3600000d);
          break;
        case 43:
          reading.getCounterPhase2().setReactivePower(val / 3600000d);
          break;
        case 44:
          reading.getCounterPhase2().setNegativeReactivePower(val / 3600000d);
          break;
        case 49:
          reading.getCounterPhase2().setApparentPower(val / 3600000d);
          break;
        case 50:
          reading.getCounterPhase2().setNegativeApparentPower(val / 3600000d);
          break;

        case 61:
          reading.getCounterPhase3().setActivePower(val / 3600000d);
          break;
        case 62:
          reading.getCounterPhase3().setNegativeActivePower(val / 3600000d);
          break;
        case 63:
          reading.getCounterPhase3().setReactivePower(val / 3600000d);
          break;
        case 64:
          reading.getCounterPhase3().setNegativeReactivePower(val / 3600000d);
          break;
        case 69:
          reading.getCounterPhase3().setApparentPower(val / 3600000d);
          break;
        case 70:
          reading.getCounterPhase3().setNegativeApparentPower(val / 3600000d);
          break;
      }
    } else if (typ == TYPE_MEASURE) {
      switch (idx) {
        case 1:
          reading.getCurrentSum().setActivePower(val * 0.1d); // W
          break;
        case 2:
          reading.getCurrentSum().setNegativeActivePower(val * 0.1d); // W
          break;
        case 3:
          reading.getCurrentSum().setReactivePower(val * 0.1d); // W
          break;
        case 4:
          reading.getCurrentSum().setNegativeReactivePower(val * 0.1d); // W
          break;
        case 9:
          reading.getCurrentSum().setApparentPower(val * 0.1d); // W
          break;
        case 10:
          reading.getCurrentSum().setNegativeApparentPower(val * 0.1d); // W
          break;
        case 13:
          reading.getCurrentSum().setPowerFactor(val * 0.001d); // cosphi
          break;
        case 21:
          reading.getCurrentPhase1().setActivePower(val * 0.1d); // W
          break;
        case 22:
          reading.getCurrentPhase1().setNegativeActivePower(val * 0.1d); // W
          break;
        case 23:
          reading.getCurrentPhase1().setReactivePower(val * 0.1d); // W
          break;
        case 24:
          reading.getCurrentPhase1().setNegativeReactivePower(val * 0.1d); // W
          break;
        case 29:
          reading.getCurrentPhase1().setApparentPower(val * 0.1d); // W
          break;
        case 30:
          reading.getCurrentPhase1().setNegativeApparentPower(val * 0.1d); // W
          break;
        case 31:
          reading.getCurrentPhase1().setElectricCurrent(val / 1000d); // A
          break;
        case 32:
          reading.getCurrentPhase1().setVoltage(val / 1000d); // V
          break;
        case 41:
          reading.getCurrentPhase2().setActivePower(val * 0.1d); // W
          break;
        case 42:
          reading.getCurrentPhase2().setNegativeActivePower(val * 0.1d); // W
          break;
        case 43:
          reading.getCurrentPhase2().setReactivePower(val * 0.1d); // W
          break;
        case 44:
          reading.getCurrentPhase2().setNegativeReactivePower(val * 0.1d); // W
          break;
        case 49:
          reading.getCurrentPhase2().setApparentPower(val * 0.1d); // W
          break;
        case 50:
          reading.getCurrentPhase2().setNegativeApparentPower(val * 0.1d); // W
          break;
        case 51:
          reading.getCurrentPhase2().setElectricCurrent(val / 1000d); // A
          break;
        case 52:
          reading.getCurrentPhase2().setVoltage(val / 1000d); // V
          break;
        case 61:
          reading.getCurrentPhase3().setActivePower(val * 0.1d); // W
          break;
        case 62:
          reading.getCurrentPhase3().setNegativeActivePower(val * 0.1d); // W
          break;
        case 63:
          reading.getCurrentPhase3().setReactivePower(val * 0.1d); // W
          break;
        case 64:
          reading.getCurrentPhase3().setNegativeReactivePower(val * 0.1d); // W
          break;
        case 69:
          reading.getCurrentPhase3().setApparentPower(val * 0.1d); // W
          break;
        case 70:
          reading.getCurrentPhase3().setNegativeApparentPower(val * 0.1d); // W
          break;
        case 71:
          reading.getCurrentPhase3().setElectricCurrent(val / 1000d); // A
          break;
        case 72:
          reading.getCurrentPhase3().setVoltage(val / 1000d); // V
          break;
      }
    }
  }

  @Override
  public boolean acceptInboundMessage(Object msg) throws Exception {
    var accept = super.acceptInboundMessage(msg);
    if (accept) {
      accept = checkSMAHeader((DatagramPacket) msg);
    }
    return accept;
  }

  private boolean checkSMAHeader(DatagramPacket msg) {
    boolean accept;
    var buffer = msg.content();
    if (buffer.readableBytes() >= 3) {
      String smaHeader = buffer.readBytes(3).toString(CharsetUtil.US_ASCII);
      accept = Objects.equals("SMA", smaHeader);
    } else {
      accept = false;
    }
    buffer.resetReaderIndex();
    return accept;
  }
}
