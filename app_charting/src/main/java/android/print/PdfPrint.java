package android.print;

import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;

import io.reactivex.Single;
import timber.log.Timber;

public class PdfPrint {

  private final PrintAttributes printAttributes;

  public PdfPrint(PrintAttributes printAttributes) {
    this.printAttributes = printAttributes;
  }

  public Single<File> save(PrintDocumentAdapter printAdapter, final File path, final String fileName) {
    return Single.<File>create(e -> {
      printAdapter.onLayout(null, printAttributes, null, new PrintDocumentAdapter.LayoutResultCallback() {
        @Override
        public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
          try {
            File file = getOutputFile(path, String.format("%s.pdf", fileName));
            Timber.d("Saving: %s", file.getAbsoluteFile());

            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
            printAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, fd, new CancellationSignal(), new PrintDocumentAdapter.WriteResultCallback() {
              @Override
              public void onWriteFinished(PageRange[] pages) {
                super.onWriteFinished(pages);
                e.onSuccess(file);
              }
            });
          } catch (Exception exception) {
            e.onError(exception);
          }
        }
      }, null);
    }).doOnSuccess(f -> Timber.d("Done saving file"));
  }

  private File getOutputFile(File path, String fileName) throws Exception {
    if (!path.exists()) {
      path.mkdirs();
    }
    File file = new File(path, fileName);
    file.createNewFile();
    return file;
  }
}
