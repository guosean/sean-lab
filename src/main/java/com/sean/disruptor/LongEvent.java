package com.sean.disruptor;

import com.google.common.base.MoreObjects;

/**
 * Created by guozhenbin on 2017/4/25.
 */
public class LongEvent {

    private long value;

    public void set(long value)
    {
        this.value = value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(LongEvent.class).add("value",value).toString();
    }
}
