//package repairer;
//
//import java.util.concurrent.locks.ReentrantLock;
//
//
//public class LockTest {
//    public static void main(String[] args) {
//      ReentrantLock rLock = new ReentrantLock();
//      Thread t1 = new Thread(new Display("Thread-1", rLock));
//      Thread t2 = new Thread(new Display("Thread-2", rLock));
//      System.out.println("starting threads ");
//      t1.start();
//      t2.start();
//    }
//
//  class Display implements Runnable {
//    private String threadName;
//    ReentrantLock lock;
//    ReentrantLock lock1;
//    Display(String threadName, ReentrantLock lock){
//      this.threadName = threadName;
//      this.lock = lock;
//    }
//    @Override
//    public void run() {
//        System.out.println("In Display run method, thread " + threadName +
//        " is waiting to get lock");
//        lock.unlock();
//        //acquiring lock
//        lock.lock();
//        lock.lock();
//        lock1.unlock();
//        lock1.lock();
//        a.lock();
//        // a.lock();
//        System.out.println("Thread " + threadName + "has got lock");
//        methodA();
//        lock.unlock();
//        lock.unlock();
//        lock1.unlock();
//    }
//
//    public void methodA(){
//        System.out.println("In Display methodA, thread " + threadName +
//        " is waiting to get lock");
//
//        lock.lock();
//        System.out.println("Thread " + threadName + "has got lock");
//        System.out.println("Count of locks held by thread " + threadName +
//         " - " + lock.getHoldCount());
//        lock.unlock();
//
//        // Not calling unlock
//        // lock.unlock();
//    }
//  }
//}