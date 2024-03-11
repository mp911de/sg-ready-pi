package cat.joanpujol.smasolar.emeter.impl;

import cat.joanpujol.smasolar.emeter.EMeterConfig;
import cat.joanpujol.smasolar.emeter.EMeterContentDecoder;
import cat.joanpujol.smasolar.emeter.EMeterLecture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import java.net.NetworkInterface;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create an observable that provides Emeter lectures. This observable is cold and can only be
 * subscribed once. When observable is subscribed a datagram channel is created that listens to
 * Emeter multicast group
 */
public class EMeterCreateObservableImpl implements ObservableOnSubscribe<EMeterLecture> {
  private static final Logger logger = LoggerFactory.getLogger(EMeterCreateObservableImpl.class);

  private EMeterConfig config;
  private boolean initialized = false;
  private ObservableEmitter<EMeterLecture> emitter;
  private AbstractChannel channel;

  public EMeterCreateObservableImpl(EMeterConfig config) {
    this.config = config;
  }

  public Observable<EMeterLecture> create() {
    return Observable.create(this);
  }

  @Override
  public void subscribe(ObservableEmitter<EMeterLecture> emitter) throws Exception {
    if (initialized)
      throw new IllegalStateException(
          "Can't subscribe twice, it's observable is expected to be shared");

    this.emitter = emitter;

    emitter.setCancellable(
        () -> {
          // Close channel when subscriber is unsubscribed
          if (channel != null) channel.close();
        });
    initialized = true;

    // Processor that simply sends received lectures to observer
    var processor =
        new SimpleChannelInboundHandler<EMeterLecture>() {
          @Override
          protected void channelRead0(ChannelHandlerContext ctx, EMeterLecture msg)
              throws Exception {
            emitter.onNext(msg);
          }
        };
    startMulticastChannelReceiver(processor);
  }

  /**
   * Starts a multicast channel receiver creating the channel and listening to it
   *
   * @param processor Processor used to notify lectures to observer
   */
  private void startMulticastChannelReceiver(SimpleChannelInboundHandler<EMeterLecture> processor) {
    try {
      AbstractChannel datagramChannel = createChannel(config, processor);
      this.channel = datagramChannel;
      datagramChannel
          .closeFuture()
          .addListener(
              future -> {
                if (future.isSuccess()) emitter.onComplete();
                else emitter.onError(future.cause());
              });
    } catch (InterruptedException | SocketException e) {
      logger.error("Error creating channel", e);
      emitter.onError(e);
    }
  }

  /**
   * Creates a datagram channel that listens to Emeter multicast group
   *
   * <p>Visibility increased for testing to be able to provide a mocked channel
   */
  protected AbstractChannel createChannel(
      EMeterConfig config, SimpleChannelInboundHandler<EMeterLecture> processor)
      throws SocketException, InterruptedException {
    NetworkInterface networkInterface =
        new CalculateNetworkInterface().calculateNetworkInterface(config);

    Bootstrap b =
        new Bootstrap()
            .group(config.getEventLoopGroup())
            .channelFactory(
                (ChannelFactory<NioDatagramChannel>)
                    () -> new NioDatagramChannel(InternetProtocolFamily.IPv4))
            .option(ChannelOption.IP_MULTICAST_IF, networkInterface)
            .option(ChannelOption.SO_REUSEADDR, true)
            .handler(
                new ChannelInitializer<NioDatagramChannel>() {
                  @Override
                  public void initChannel(NioDatagramChannel ch) {
                    ch.pipeline()
                        .addLast(new EMeterContentDecoder())
                        .addLast(processor)
                        // Next processors apply only for incorrect messaages that aren't handled by
                        // processor
                        .addLast(
                            new LoggingHandler(EMeterCreateObservableImpl.class, LogLevel.DEBUG))
                        .addLast(
                            new SimpleChannelInboundHandler<Object>() {
                              @Override
                              protected void channelRead0(ChannelHandlerContext ctx, Object msg)
                                  throws Exception {
                                logger.warn(
                                    "Unprocessable message received (previously logged with DEBUG level)");
                              }
                            });
                  }
                });

    NioDatagramChannel channel =
        (NioDatagramChannel) b.bind(config.getAddress().getPort()).sync().channel();
    channel.joinGroup(config.getAddress(), networkInterface).sync();
    return channel;
  }
}
