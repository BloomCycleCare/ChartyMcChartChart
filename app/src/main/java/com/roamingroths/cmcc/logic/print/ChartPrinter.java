package com.roamingroths.cmcc.logic.print;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PdfPrint;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintJob;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.roamingroths.cmcc.logic.chart.CycleRenderer;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
    mPrintAttributes = new PrintAttributes.Builder()
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .setMediaSize(PrintAttributes.MediaSize.ISO_A3.asLandscape())
        .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
        .build();
    mPrintManager = printManager;
    mPageRenderer = pageRenderer;
  }

  public static ChartPrinter create(Activity activity, Observable<CycleRenderer> renderers) {
    PrintManager printManager = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
    PageRenderer pageRenderer = new PageRenderer(renderers);
    return new ChartPrinter(pageRenderer, printManager, activity);
  }

  public Completable savePDF() {
    WebView.enableSlowWholeDocumentDraw();
    return mPageRenderer.createPages()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMapSingle(this::createWebView)
        .flatMapCompletable(webView -> {
          String jobName = String.format("Chart %s", LocalDate.now().toString());
          //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/PDFTest/");
          File path = mContext.getCacheDir();
          PdfPrint pdfPrint = new PdfPrint(mPrintAttributes);
          return pdfPrint.save(webView.createPrintDocumentAdapter(jobName), path, "saved_chart").toCompletable();
        });
  }

  public Single<List<File>> savePDFs() {
    WebView.enableSlowWholeDocumentDraw();
    return mPageRenderer.createPages()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMapSingle(this::createWebView)
        .flatMapSingle(webView -> {
          String jobName = String.format("Chart %s", LocalDate.now().toString());
          //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/PDFTest/");
          File path = mContext.getCacheDir();
          PdfPrint pdfPrint = new PdfPrint(mPrintAttributes);
          return pdfPrint.save(webView.createPrintDocumentAdapter(jobName), path, "saved_chart_" + System.currentTimeMillis());
        })
        .toList();
  }

  public Observable<PrintJob> print() {
    WebView.enableSlowWholeDocumentDraw();
    return mPageRenderer.createPages()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMapSingle(this::createWebView)
        .map(webView -> {
          String jobName = String.format("Chart %s", LocalDate.now().toString());
          return mPrintManager.print(jobName, webView.createPrintDocumentAdapter(jobName), mPrintAttributes);
        });
  }

  private Single<WebView> createWebView(String html) {
    return Single.create(emitter -> {
      WebView webView = new WebView(mContext);
      webView.setWebViewClient(new WebViewClient() {
        private WebView ref = webView;
        @Override
        public void onPageFinished(WebView view, String url) {
          emitter.onSuccess(view);
          ref = null;
        }
      });
      /*final MyWebView webView = new MyWebView(mContext);
      webView.setWebViewClient(new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
          emitter.onSuccess(view);
        }
      });*/
      webView.setPadding(0, 100,0, 0);
      Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
      webView.setInitialScale(Double.valueOf(display.getWidth() * 0.0625).intValue());
      webView.loadDataWithBaseURL("file:///android_asset/", html, "text/HTML", "UTF-8", null);
    });
  }

  private PrintedPdfDocument addPageToDocument(PrintedPdfDocument document, WebView webView) {
    int pageNum = document.getPages().size() + 1;
    PdfDocument.PageInfo pageInfo =
        new PdfDocument.PageInfo.Builder(WIDTH_IN_POINTS, HEIGHT_IN_POINTS, pageNum).create();
    PdfDocument.Page page = document.startPage(pageInfo);
    Canvas canvas = page.getCanvas();
    canvas.setDensity(Bitmap.DENSITY_NONE);
    webView.draw(canvas);
    //(webView.capturePicture()).draw(canvas);
    document.finishPage(page);
    return document;
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
