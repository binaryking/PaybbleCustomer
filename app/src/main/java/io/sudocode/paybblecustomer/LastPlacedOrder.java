package io.sudocode.paybblecustomer;

public class LastPlacedOrder {

    private static LastPlacedOrder mLastPlacedOrder;

    private CustomerOrder mLastOrder;

    public static LastPlacedOrder instance() {
        synchronized (LastPlacedOrder.class) {
            if (mLastPlacedOrder == null) {
                mLastPlacedOrder = new LastPlacedOrder();
            }
        }

        return mLastPlacedOrder;
    }

    public void setLastOrder(CustomerOrder order) {
        mLastOrder = order;
    }

    public CustomerOrder getLastOrder() {
        return mLastOrder;
    }

}
