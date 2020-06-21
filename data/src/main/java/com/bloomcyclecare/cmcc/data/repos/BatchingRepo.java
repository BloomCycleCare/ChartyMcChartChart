package com.bloomcyclecare.cmcc.data.repos;

public interface BatchingRepo {

  boolean beginBatchUpdates();

  boolean completeBatchUpdates();

}
