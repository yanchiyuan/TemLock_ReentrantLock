package repairer;

import java.util.concurrent.locks.ReentrantLock;

public class demo {

    class Depot {
        private int size;
        private ReentrantLock lock;
        Depot depot = new demo().new Depot();

        public Depot() {
            this.size = 0;
            this.lock = new ReentrantLock();
        }

        public void produce(int newSize) {
            lock.lock();
            depot.consume(newSize);
            try {
                size += newSize;
                System.out.println(Thread.currentThread().getName() + size);
            } finally {
                lock.unlock();
            }
        }

        public void consume(int newSize) {
            lock.lock();
            try {
                size -= newSize;
                System.out.println(Thread.currentThread().getName() + size);
            } finally {
                lock.unlock();
                //lock.unlock();
            }
        }
    }

    class Producer {
        private Depot depot;

        public Producer(Depot depot) {
            this.depot = depot;
        }

        public void produce(final int newSize) {
            new Thread() {
                @Override
                public void run() {
                    depot.produce(newSize);
                }
            }.start();
        }
    }

    class Customer {
        private Depot depot;

        public Customer(Depot depot) {
            this.depot = depot;
        }

        public void consume(final int newSize) {
            new Thread() {
                @Override
                public void run() {
                    depot.consume(newSize);
                }
            }.start();
        }
    }

    public static void main(String[] args) {

        Depot depot = new demo().new Depot();
        Producer producer = new demo().new Producer(depot);
        Customer customer = new demo().new Customer(depot);

        producer.produce(60);
        producer.produce(120);
        customer.consume(90);
        customer.consume(150);
        producer.produce(110);
    }
}