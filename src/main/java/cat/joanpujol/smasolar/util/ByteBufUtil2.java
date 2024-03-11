package cat.joanpujol.smasolar.util;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;

public class ByteBufUtil2 {

  public static final boolean allZero(ByteBuf byteBuf) {
    return byteBuf.forEachByte(ByteProcessor.FIND_NON_NUL) == -1;
  }
}
