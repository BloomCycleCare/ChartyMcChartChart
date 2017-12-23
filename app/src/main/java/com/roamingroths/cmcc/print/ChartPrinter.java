package com.roamingroths.cmcc.print;

import android.app.Activity;
import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintJob;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.webkit.WebView;

import com.roamingroths.cmcc.data.ChartEntryList;

import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by parkeroth on 11/26/17.
 */

public class ChartPrinter {

  private final Context mContext;
  private final PrintAttributes mPrintAttributes;
  private final PrintManager mPrintManager;
  private final PageRenderer mPageRenderer;

  public ChartPrinter(PageRenderer pageRenderer, PrintManager printManager, Context context) {
    mContext = context;
    mPrintAttributes = new PrintAttributes.Builder().setMinMargins(PrintAttributes.Margins.NO_MARGINS).setMediaSize(PrintAttributes.MediaSize.ISO_A3.asLandscape()).build();
    mPrintManager = printManager;
    mPageRenderer = pageRenderer;
  }

  public static ChartPrinter create(Activity activity, Observable<ChartEntryList> lists) {
    PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
    PageRenderer pageRenderer = new PageRenderer(lists);
    return new ChartPrinter(pageRenderer, printManager, activity);
  }

  public Single<PrintJob> print() {
    WebView.enableSlowWholeDocumentDraw();
    return mPageRenderer.createPages()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap(createWebViews())
        .scan(new PrintedPdfDocument(mContext, mPrintAttributes), addPageToDocument())
        .lastOrError()
        .map(new Function<PrintedPdfDocument, PrintJob>() {
          @Override
          public PrintJob apply(PrintedPdfDocument pdfDocument) throws Exception {
            PdfPrintAdapter adapter = new PdfPrintAdapter(pdfDocument);
            return mPrintManager.print("TestDocument", adapter, mPrintAttributes);
          }
        });
  }

  private Function<String, ObservableSource<WebView>> createWebViews() {
    return new Function<String, ObservableSource<WebView>>() {
      @Override
      public ObservableSource<WebView> apply(final String html) throws Exception {
        Log.v("TAG", html);
        final MyWebView webView = new MyWebView(mContext);
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/HTML", "UTF-8", null);
        return webView.source().toObservable();
      }
    };
  }

  private BiFunction<PrintedPdfDocument, WebView, PrintedPdfDocument> addPageToDocument() {
    return new BiFunction<PrintedPdfDocument, WebView, PrintedPdfDocument>() {
      @Override
      public PrintedPdfDocument apply(PrintedPdfDocument printedPdfDocument, WebView webView) throws Exception {
        int pageNum = printedPdfDocument.getPages().size() + 1;
        PdfDocument.PageInfo pageInfo =
            new PdfDocument.PageInfo.Builder(WIDTH_IN_POINTS, HEIGHT_IN_POINTS, pageNum).create();
        PdfDocument.Page page = printedPdfDocument.startPage(pageInfo);
        (webView.capturePicture()).draw(page.getCanvas());
        printedPdfDocument.finishPage(page);
        return printedPdfDocument;
      }
    };
  }

  private static final int POINTS_PER_INCH = 72;
  private static final int HEIGHT_IN_POINTS = 11 * POINTS_PER_INCH;
  private static final int WIDTH_IN_POINTS = 17 * POINTS_PER_INCH;

  private static class PdfPrintAdapter extends PrintDocumentAdapter {

    private final PrintedPdfDocument mPdfDocument;

    public PdfPrintAdapter(PrintedPdfDocument document) {
      super();
      mPdfDocument = document;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle bundle) {
      // TODO: Create a new PdfDocument with the requested page attributes

      // Respond to cancellation request
      if (cancellationSignal.isCanceled() ) {
        callback.onLayoutCancelled();
        return;
      }

      // Compute the expected number of printed pages
      int pages = mPdfDocument.getPages().size();

      if (pages > 0) {
        // Return print information to print framework
        PrintDocumentInfo info = new PrintDocumentInfo
            .Builder("print_output.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pages)
            .build();
        // Content layout reflow is complete
        callback.onLayoutFinished(info, true);
      } else {
        // Otherwise report an error to the print framework
        callback.onLayoutFailed("Page count calculation failed.");
      }
    }

    @Override
    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
      // TODO: check page ranges

      // check for cancellation
      if (cancellationSignal.isCanceled()) {
        callback.onWriteCancelled();
        mPdfDocument.close();
        return;
      }

      // Write PDF document to file
      try {
        mPdfDocument.writeTo(new FileOutputStream(
            destination.getFileDescriptor()));
      } catch (IOException e) {
        callback.onWriteFailed(e.toString());
        return;
      }

      // Signal the print framework the document is complete
      callback.onWriteFinished(pageRanges);
    }
  }

  private static class MyWebView extends WebView {

    private final Subject<Integer> mHeightSubject;

    public MyWebView(Context context) {
      super(context);
      mHeightSubject = BehaviorSubject.create();
    }

    @Override
    public void invalidate() {
      super.invalidate();
      Log.v("MyWebView", "invalidate: " + getContentHeight());
      mHeightSubject.onNext(getContentHeight());
    }

    public Single<WebView> source() {
      return mHeightSubject
          .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer integer) throws Exception {
              return integer > 0;
            }
          })
          .firstOrError()
          .map(new Function<Integer, WebView>() {
            @Override
            public WebView apply(Integer integer) throws Exception {
              return MyWebView.this;
            }
          });
    }
  }
}
