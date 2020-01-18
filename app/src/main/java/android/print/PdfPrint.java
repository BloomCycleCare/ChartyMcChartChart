package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;

import io.reactivex.Completable;

public class PdfPrint {

  private static final String TAG = PdfPrint.class.getSimpleName();
  private final PrintAttributes printAttributes;

  public PdfPrint(PrintAttributes printAttributes) {
    this.printAttributes = printAttributes;
  }

  public Completable print(PrintDocumentAdapter printAdapter, final File path, final String fileName) {
    return Completable.create(e -> {
      printAdapter.onLayout(null, printAttributes, null, new PrintDocumentAdapter.LayoutResultCallback() {
        @Override
        public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
          try {
            printAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, getOutputFile(path, fileName), new CancellationSignal(), new PrintDocumentAdapter.WriteResultCallback() {
              @Override
              public void onWriteFinished(PageRange[] pages) {
                super.onWriteFinished(pages);
                e.onComplete();
              }
            });
          } catch (Exception exception) {
            e.onError(exception);
          }
        }
      }, null);
    });
  }

  private ParcelFileDescriptor getOutputFile(File path, String fileName) throws Exception {
    if (!path.exists()) {
      path.mkdirs();
    }
    File file = new File(path, fileName);
    file.createNewFile();
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
  }
}
