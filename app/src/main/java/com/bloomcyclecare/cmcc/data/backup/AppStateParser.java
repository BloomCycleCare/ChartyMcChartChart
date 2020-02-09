package com.bloomcyclecare.cmcc.data.backup;

import com.bloomcyclecare.cmcc.data.models.AppState;
import com.bloomcyclecare.cmcc.utils.GsonUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import io.reactivex.Single;

public class AppStateParser {

  public static Single<AppState> parse(Callable<InputStream> in) {
    return Single
        .fromCallable(in)
        .map(inputStream -> {
          BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
          StringBuilder total = new StringBuilder();
          String line;
          while ((line = r.readLine()) != null) {
            total.append(line);
          }
          return GsonUtil.getGsonInstance().fromJson(total.toString(), AppState.class);
        });
  }
}
