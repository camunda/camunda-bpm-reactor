/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.camunda.bpm.extension.reactor.projectreactor.publisher;

import org.camunda.bpm.extension.reactor.projectreactor.Bus;
import org.camunda.bpm.extension.reactor.projectreactor.EventBus;
import org.camunda.bpm.extension.reactor.projectreactor.registry.Registration;
import org.camunda.bpm.extension.reactor.projectreactor.selector.Selector;
import org.camunda.bpm.extension.reactor.projectreactor.Dispatcher;
import org.camunda.bpm.extension.reactor.projectreactor.dispatch.SynchronousDispatcher;
import org.camunda.bpm.extension.reactor.projectreactor.reactivestreams.SerializedSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Emit signals whenever an Event arrives from the {@link Selector} topic from the {@link Bus}.
 * This stream will never emit a {@link org.reactivestreams.Subscriber#onComplete()}.
 * <p>
 * Create such stream with the provided factory, E.g.:
 * <pre>
 * {@code Streams.create(eventBus.on($("topic"))).consume(System.out::println)}
 * </pre>
 *
 * @author Stephane Maldini
 */
public final class BusPublisher<T> implements Publisher<T> {

  private final Selector selector;
  private final Bus<T> observable;
  private final boolean ordering;

  public BusPublisher(final @Nonnull Bus<T> observable,
                      final @Nonnull Selector selector) {

    this.selector = selector;
    this.observable = observable;
    Dispatcher dispatcher = EventBus.class.isAssignableFrom(observable.getClass()) ?
      ((EventBus) observable).getDispatcher() : SynchronousDispatcher.INSTANCE;
    this.ordering = dispatcher.supportsOrdering();
  }

  @Override
  public void subscribe(Subscriber<? super T> s) {
    final Subscriber<? super T> subscriber;
    if (!ordering) {
      subscriber = SerializedSubscriber.create(s);
    } else {
      subscriber = s;
    }

    subscriber.onSubscribe(new Subscription() {

      final Registration<Object, Consumer<? extends T>> registration = observable.on(selector, event -> subscriber.onNext(event));

      @Override
      public void request(long n) {
        //IGNORE
      }

      @Override
      public void cancel() {
        registration.cancel();
      }
    });
  }

  @Override
  public String toString() {
    return "BusPublisher{" +
      "selector=" + selector +
      ", bus=" + observable +
      '}';
  }
}
