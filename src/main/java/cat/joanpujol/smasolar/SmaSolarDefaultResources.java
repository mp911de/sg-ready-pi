package cat.joanpujol.smasolar;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Shared resouces that are used as default configuration if not otherwise configured ones are
 * provided.
 *
 * <p>These resources should be released when the JVM is shutting down or the ClassLoader that
 * loaded us is unloaded. See {@link #releaseSharedResources()}.
 *
 * <p>Holders are used to lazy create resources only when first used and not when this class is
 * loaded
 */
public abstract class SmaSolarDefaultResources {

  public static EventLoopGroup sharedEventLoop() {
    return EventLoopHolder.EventLoop;
  }

  /** Shutdown/stop any shared resources that may be in use. */
  public static void releaseSharedResources() {
    sharedEventLoop().shutdownGracefully();
  }

  private static class EventLoopHolder {
    private static final EventLoopGroup EventLoop = new NioEventLoopGroup();
  }
}
