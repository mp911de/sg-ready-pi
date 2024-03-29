package cat.joanpujol.smasolar.emeter;

import cat.joanpujol.smasolar.emeter.impl.EMeterCreateObservableImpl;
import reactor.core.publisher.Flux;

/** Provides an interface to cotinously observe emeter lectures by joining Emeter multicast group */
public class EMeterReader {

  private EMeterConfig config;

  /** Creates an instance using default configuration */
  public EMeterReader() {
    this(EMeterConfig.newBuilder().build());
  }

  /**
   * Creates an instance usen given config
   *
   * @param config Configuration
   */
  public EMeterReader(EMeterConfig config) {
    this.config = config;
  }

  /**
   * Creates a cold observable that starts to listens to EMeter lectures on first subscription. The
   * observable can be shared to multiple subscribers that will receive same results without
   * creating multiple sockets.
   *
   * <p>Subscribed observers should not do blocking operations or must use it's own scheduler using
   * {@link Observable#observeOn(Scheduler)}}
   */
  public final Flux<EMeterLecture> create() {
    return create().share();
  }

  // Exposed only for testing purpouses
  protected EMeterCreateObservableImpl createObservable() {
    return new EMeterCreateObservableImpl(config);
  }

  protected final EMeterConfig getConfig() {
    return config;
  }

  public static void main(String[] args) {
    new EMeterReader().create().subscribe(lecture -> System.out.println(lecture));
  }
}
