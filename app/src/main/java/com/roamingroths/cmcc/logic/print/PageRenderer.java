package com.roamingroths.cmcc.logic.print;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.roamingroths.cmcc.logic.chart.CycleRenderer;
import com.roamingroths.cmcc.logic.chart.StickerColor;
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

  private final Observable<CycleRenderer> renderers;

  public PageRenderer(Observable<CycleRenderer> renderers) {
    this.renderers = renderers;
  }

  public Observable<String> createPages() {
    return renderers.flatMap(createCycleRows()).buffer(NUM_ROWS_PER_PAGE).map(createPage());
  }

  public static int numRowsPerPage() {
    return NUM_ROWS_PER_PAGE;
  }

  public static int numRows(int numEntries) {
    return numFullRows(numEntries) + (hasPartialRow(numEntries) ? 1 : 0);
  }

  private static int numFullRows(int numEntries) {
    return numEntries / NUM_DAYS_PER_CHART;
  }

  private static boolean hasPartialRow(int numEntries) {
    return numEntries % NUM_DAYS_PER_CHART != 0;
  }

  private static Function<CycleRenderer, ObservableSource<String>> createCycleRows() {
    return (Function<CycleRenderer, ObservableSource<String>>) renderer -> {
      List<CycleRenderer.RenderableEntry> renderableEntries = renderer.render();
      List<String> rows = new ArrayList<>();
      int numFullRows = numFullRows(renderableEntries.size());
      boolean hasPartialRow = hasPartialRow(renderableEntries.size());
      for (int i=0; i < numFullRows; i++) {
        StringBuilder builder = new StringBuilder();
        int startIndex = i * NUM_DAYS_PER_CHART;
        int endIndex = startIndex + NUM_DAYS_PER_CHART - 1;
        appendCycle(builder, renderableEntries, startIndex, endIndex);
        rows.add(builder.toString());
      }
      if (hasPartialRow) {
        StringBuilder builder = new StringBuilder();
        int startIndex = numFullRows * NUM_DAYS_PER_CHART;
        int endIndex = renderableEntries.size() - 1;
        appendCycle(builder, renderableEntries, startIndex, endIndex);
        rows.add(builder.toString());
      }
      return Observable.fromIterable(rows);
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
        String out = html.toString();
        return html.toString();
      }
    };
  }

  private static void appendHead(StringBuilder builder) {
    builder.append("<head>");
    builder.append("<style>");
    builder.append("#container { padding-top: 20pt; padding-left: 20pt; }");
    builder.append("table.outer { margin: auto; table-layout: fixed; width: 100%; border: 4px solid black; border-collapse: collapse; } ");
    builder.append("table.sticker { height: 100%; width: 100%; } ");
    builder.append("tr.stickers { border-top: 4px solid black; font-size: 150%; font-style: bold; } ");
    builder.append("td.cell { text-align: center; height: 66pt; width: 34pt; border: 1px solid black; } ");
    builder.append("tr.day-num td { height: 34pt; } ");
    builder.append("table.sticker tr { height: 33% } ");
    builder.append("table.sticker td { text-align: center; font-weight: bold; } ");
    builder.append("td.stats { padding-left: 2pt } ");
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
    builder.append("<div id=\"container\">");
    builder.append("<table class=\"outer\">");
    appendDays(builder);
    for (String row : cycleRows) {
      builder.append(row);
    }
    for (int i=cycleRows.size(); i < NUM_ROWS_PER_PAGE; i++) {
      appendEmptyCycle(builder);
    }
    builder.append("</table>");
    builder.append("</div>");
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
    classes.add("cell");
    classes.addAll(extraClasses);
    if (index % 7 == 6) {
      classes.add("separator");
    }
    StringBuilder builder = new StringBuilder();
    builder.append("<td valign=\"top\"");
    if (!classes.isEmpty()) {
      builder.append(" class=\"").append(ON_SPACE.join(classes)).append("\"");
    }
    builder.append(">");
    return builder.toString();
  }

  private static String getColorClass(StickerColor color) {
    switch (color) {
      case RED:
        return "red";
      case YELLOW:
        return "yellow";
      case WHITE:
      case GREY:
        return "white";
      case GREEN:
        return "green";
      default:
        throw new IllegalArgumentException("Unkown color id: " + color);
    }
  }

  private static void appendCycle(StringBuilder builder, List<CycleRenderer.RenderableEntry> renderableEntries, int startIndex, int endIndex) {
    Preconditions.checkArgument(startIndex >= 0 && endIndex < renderableEntries.size());
    appendStickers(builder, renderableEntries, startIndex, endIndex);
    appendEntries(builder, renderableEntries, startIndex, endIndex);
  }

  private static void appendEmptyCycle(StringBuilder builder) {
    // Starting at 1 and ending at 0 is a horrible hack and I only have myself to blame when this
    // causes problems later.
    appendStickers(builder, null, 1, 0);
    appendEntries(builder, null, 1, 0);
  }

  private static void appendStickers(StringBuilder builder, @Nullable List<CycleRenderer.RenderableEntry> renderableEntries, int startIndex, int endIndex) {
    builder.append("<tr class=\"stickers\">");
    if (renderableEntries != null) {
      for (int i = startIndex; i <= endIndex; i++) {
        CycleRenderer.RenderableEntry entry = renderableEntries.get(i);

        int numLines = 3;
        String[] textLines = new String[numLines];
        for (int l=0; l<numLines; l++) {
          textLines[l] = "";
        }

        switch (entry.intercourseTimeOfDay) {
          case ANY:
            textLines[0] = "I";
            break;
          case END:
            textLines[2] = "I";
            break;
          case NONE:
          default:
            break;
        }

        textLines[1] = entry.peakDayText;

        List<String> classes = new ArrayList<>();
        classes.add(getColorClass(entry.backgroundColor));
        if (entry.showBaby) {
          classes.add("baby");
        }

        builder.append(openCellTag(i, classes));
        builder.append("<table class=\"sticker\">");
        for (int l=0; l<numLines; l++) {
          builder.append("<tr><td valign=\"top\">");
          builder.append(textLines[l]);
          builder.append("</td></tr>");
        }
        builder.append("</table>");
        builder.append("</td>");
      }
    }
    fillEmptyDays(builder, startIndex, endIndex);
    builder.append("<td class=\"stats\" rowspan=\"2\" valign=\"top\">");
    if (startIndex == 0 && renderableEntries != null) {
      //fillStats(builder, entryList.getStats());
    }
    builder.append("</td>");
    builder.append("</tr>");
  }

  private static void appendEntries(StringBuilder builder, List<CycleRenderer.RenderableEntry> renderableEntries, int startIndex, int endIndex) {
    builder.append("<tr>");
    if (renderableEntries != null) {
      for (int i = startIndex; i <= endIndex; i++) {
        CycleRenderer.RenderableEntry entry = renderableEntries.get(i);
        List<String> lines = new ArrayList<>();
        lines.add(DateUtil.toPrintStr(entry.modificationContext.entry.entryDate));
        List<String> summaryPieces = Lists.newArrayList(entry.entrySummary.split(" "));
        if (summaryPieces.size() > 0 && summaryPieces.get(summaryPieces.size() - 1).equals("I")) {
          summaryPieces.remove(summaryPieces.size() - 1);
        }
        lines.addAll(summaryPieces);
        if (!Strings.isNullOrEmpty(entry.pocSummary)) {
          lines.add(entry.pocSummary);
        }
        while (lines.size() < 4) {
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
