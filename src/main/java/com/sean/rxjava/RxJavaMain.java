package com.sean.rxjava;

import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.internal.schedulers.SingleScheduler;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.BehaviorSubject;

import java.io.IOException;
import java.util.List;

/**
 * Created by guozhenbin on 2017/3/16.
 * 1. observer pattern
 * 2. async
 * 3. multithread
 */
public class RxJavaMain {

    public static void main(String[] args) throws IOException {
        simpleCase();
//        subjectCase();
//        schedulerCase();
        operatorCase();

    }

    private static void simpleCase() {
        Flowable.just("d").subscribe(System.out::println);
        List<String> list = Lists.newArrayList("1", "2", "3", "4");
        Observable.fromIterable(list).subscribe(System.out::println);
        Observable.fromIterable(list).zipWith(Observable.range(1, 3), (string, count) -> String.format("%s:%d", string, count)).forEach(System.out::println);
    }

    private static void subjectCase() {
        AsyncSubject as = AsyncSubject.create();
        as.onNext("as1");
        as.onNext("as2");
        as.onComplete();
        as.subscribe(System.out::println);
        as.onNext("as3");

        BehaviorSubject bs = BehaviorSubject.create();
        bs.onNext("bs1");
        bs.onNext("bs2");

        bs.subscribe(System.out::println);
        bs.onNext("bs3");
        bs.onComplete();
    }

    public static void schedulerCase() throws IOException {
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {

            public void subscribe(ObservableEmitter<String> e) throws Exception {
                e.onNext("test");
            }
        }).observeOn(new SingleScheduler());
        observable.subscribe( s -> {
                System.out.println(s);
                System.out.println(Thread.currentThread().getName());
        });
        while (System.in.read() != -1) {
            System.exit(0x0);
        }

    }

    public static void operatorCase() {
        Observable o1 = Observable.just("1", "2");
        System.out.println(o1.contains("2").blockingGet());
    }

}