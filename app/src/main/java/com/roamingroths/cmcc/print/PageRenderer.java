package com.roamingroths.cmcc.print;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.roamingroths.cmcc.R;
import com.roamingroths.cmcc.data.ChartEntryList;
import com.roamingroths.cmcc.logic.ChartEntry;
import com.roamingroths.cmcc.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.Nullable;
import io.reactivex.functions.Function;

/**
 * Created by parkeroth on 12/23/17.
 */

public class PageRenderer {

  private static final int NUM_ROWS_PER_PAGE = 6;
  private static final int NUM_DAYS_PER_CHART = 35;
  private static final Joiner ON_SPACE = Joiner.on(" ");
  private static final Joiner ON_BR = Joiner.on("<br>");

  private final Observable<ChartEntryList> mLists;

  public PageRenderer(Observable<ChartEntryList> mLists) {
    this.mLists = mLists;
  }

  public Observable<String> createPages() {
    return mLists.flatMap(createCycleRows()).buffer(NUM_ROWS_PER_PAGE).map(createPage());
  }

  private static Function<ChartEntryList, ObservableSource<String>> createCycleRows() {
    return new Function<ChartEntryList, ObservableSource<String>>() {
      @Override
      public ObservableSource<String> apply(ChartEntryList chartEntryList) throws Exception {
        List<String> rows = new ArrayList<>();
        int numFullRows = chartEntryList.size() / NUM_DAYS_PER_CHART;
        boolean hasPartialRow = chartEntryList.size() % NUM_DAYS_PER_CHART != 0;
        for (int i=0; i < numFullRows; i++) {
          StringBuilder builder = new StringBuilder();
          int startIndex = i * NUM_DAYS_PER_CHART;
          int endIndex = startIndex + NUM_DAYS_PER_CHART - 1;
          appendCycle(builder, chartEntryList, startIndex, endIndex);
          rows.add(builder.toString());
        }
        if (hasPartialRow) {
          StringBuilder builder = new StringBuilder();
          int startIndex = numFullRows * NUM_DAYS_PER_CHART;
          int endIndex = chartEntryList.size() - 1;
          appendCycle(builder, chartEntryList, startIndex, endIndex);
          rows.add(builder.toString());
        }
        return Observable.fromIterable(rows);
      }
    };
  }

  private static Function<List<String>, String> createPage() {
    return new Function<List<String>, String>() {
      @Override
      public String apply(List<String> cycleRows) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<html>");
        appendHead(html);
        appendBody(html, cycleRows);
        html.append("</html>");
        return html.toString();
      }
    };
  }

  private static void appendHead(StringBuilder builder) {
    builder.append("<head>");
    builder.append("<style>");
    builder.append("table { table-layout: fixed; width: 100%; border: 4px solid black; border-collapse: collapse; } ");
    builder.append("tr.stickers { border-top: 4px solid black; font-size: 150%; font-style: bold; } ");
    builder.append("td { text-align: center; height: 70pt; width: 36pt; border: 1px solid black; } ");
    builder.append("tr.day-num td { height: 30px; } ");
    builder.append("td.red { background: red; } ");
    builder.append("td.yellow { background: yellow; } ");
    builder.append("td.green { background: green; } ");
    builder.append("td.separator { border-right: 4px solid black; } ");
    builder.append("td.baby { background-image: url(\"ic_child_care_black_24px.svg\"); background-position: center; background-repeat: no-repeat; } ");
    builder.append("</style>");
    builder.append("</head>");
  }

  private static void appendBody(StringBuilder builder, List<String> cycleRows) {
    Preconditions.checkArgument(cycleRows.size() <= NUM_ROWS_PER_PAGE);
    builder.append("<table>");
    appendDays(builder);
    for (String row : cycleRows) {
      builder.append(row);
    }
    for (int i=cycleRows.size(); i < NUM_ROWS_PER_PAGE; i++) {
      appendEmptyCycle(builder);
    }
    builder.append("</table>");
  }

  private static void appendDays(StringBuilder builder) {
    builder.append("<tr class=\"day-num\">");
    for (int i=0; i<NUM_DAYS_PER_CHART; i++) {
      builder.append(openCellTag(i)).append(i+1).append("</td>");
    }
    builder.append("</tr>");
  }

  private static String openCellTag(int index) {
    return openCellTag(index, ImmutableList.<String>of());
  }

  private static String openCellTag(int index, List<String> extraClasses) {
    List<String> classes = new ArrayList<>();
    classes.addAll(extraClasses);
    if (index % 7 == 6) {
      classes.add("separator");
    }
    StringBuilder builder = new StringBuilder();
    builder.append("<td");
    if (!classes.isEmpty()) {
      builder.append(" class=\"").append(ON_SPACE.join(classes)).append("\"");
    }
    builder.append(">");
    return builder.toString();
  }

  private static String getColorClass(ChartEntryList entryList, ChartEntry entry) {
    int colorId = entryList.getEntryColorResource(entry.observationEntry);
    switch (colorId) {
      case R.color.entryRed:
        return "red";
      case R.color.entryYellow:
        return "yellow";
      case R.color.entryWhite:
      case R.color.entryGrey:
        return "white";
      case R.color.entryGreen:
        return "green";
      default:
        throw new IllegalArgumentException("Unkown color id: " + colorId);
    }
  }

  private static void appendCycle(StringBuilder builder, ChartEntryList entryList, int startIndex, int endIndex) {
    Preconditions.checkArgument(startIndex >= 0 && endIndex < entryList.size());
    appendStickers(builder, entryList, startIndex, endIndex);
    appendEntries(builder, entryList, startIndex, endIndex);
  }

  private static void appendEmptyCycle(StringBuilder builder) {
    // Starting at 1 and ending at 0 is a horrible hack and I only have myself to blame when this
    // causes problems later.
    appendStickers(builder, null, 1, 0);
    appendEntries(builder, null, 1, 0);
  }

  private static void appendStickers(StringBuilder builder, @Nullable ChartEntryList entryList, int startIndex, int endIndex) {
    builder.append("<tr class=\"stickers\">");
    if (entryList != null) {
      for (int i = startIndex; i <= endIndex; i++) {
        ChartEntry entry = entryList.get(entryList.size() - 1 - i);

        List<String> classes = new ArrayList<>();
        classes.add(getColorClass(entryList, entry));
        if (entryList.shouldShowBaby(entry)) {
          classes.add("baby");
        }

        builder.append(openCellTag(i, classes));

        List<String> textItems = new ArrayList<>();
        if (entry.observationEntry != null && entry.observationEntry.intercourse) {
          textItems.add("I");
        }
        if (entry.observationEntry != null) {
          textItems.add(entryList.getPeakDayViewText(entry.observationEntry));
        }
        if (!textItems.isEmpty()) {
          builder.append(ON_SPACE.join(textItems));
        }
        builder.append("</td>");
      }
    }
    fillEmptyDays(builder, startIndex, endIndex);
    builder.append("</tr>");
  }

  private static void appendEntries(StringBuilder builder, ChartEntryList entryList, int startIndex, int endIndex) {
    builder.append("<tr>");
    if (entryList != null) {
      for (int i = startIndex; i <= endIndex; i++) {
        ChartEntry entry = entryList.get(entryList.size() - 1 - i);
        List<String> lines = new ArrayList<>();
        lines.add(DateUtil.toPrintStr(entry.entryDate));
        if (entry.observationEntry != null && entry.observationEntry.observation != null) {
          lines.addAll(Lists.newArrayList(entry.observationEntry.observation.toString().split(" ")));
        }
        while (lines.size() < 3) {
          lines.add("&nbsp;");
        }
        builder.append(openCellTag(i));
        builder.append(ON_BR.join(lines));
        builder.append("</td>");
      }
    }
    fillEmptyDays(builder, startIndex, endIndex);
    builder.append("</tr>");
  }

  private static void fillEmptyDays(StringBuilder builder, int startIndex, int endIndex) {
    int numEntriesAdded = endIndex - startIndex;
    for (int i=numEntriesAdded + 1; i < NUM_DAYS_PER_CHART; i++) {
      builder.append(openCellTag(i)).append("</td>");
    }
  }
}
