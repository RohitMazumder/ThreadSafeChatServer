package com.rohit.ThreadSafeChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Test {
	public static void main(String[] args){
		Handler handler = new Handler("rohit");
		(new Thread(handler)).start();
		Handler handler2 = new Handler("anindita");
		(new Thread(handler2)).start();
	}
}

class Handler implements Runnable {
	//private String name;
	private static /*final*/ ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	public String name;
	
	Handler(String name){
		this.name = name;
	}
	
	public void run() {
		System.out.println(this.lock);
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			try {
				fn();
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public  void fn() throws InterruptedException {
		lock.writeLock().lock();
		System.out.println("Inside fn : " + name);
		Thread.sleep(5000);
		lock.writeLock().unlock();
	}
}
