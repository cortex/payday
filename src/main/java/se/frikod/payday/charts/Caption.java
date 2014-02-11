package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Pair;

import java.util.LinkedList;
import java.util.List;

import se.frikod.payday.Transaction;
import se.frikod.payday.TransactionsGraphView;

class Caption{
    private Paint titleStyle;
    private Paint descriptionStyle;
    private Paint amountStyle;
    private Paint backgroundStyle;
    private Paint overlayStyle;
    private Paint overlayBorderStyle;
    private float frameWidth;
    private float frameHeight;

    private Rect rect;

    Caption(TransactionsGraphView view){
        float captionFontSize = 10  * view.getResources().getDisplayMetrics().density;

        titleStyle = new Paint();
        titleStyle.setColor(Color.DKGRAY);
        titleStyle.setTextSize(captionFontSize);
        titleStyle.setTypeface(Typeface.createFromAsset(view.context.getAssets(), "fonts/Roboto-Regular.ttf"));
        titleStyle.setTextAlign(Paint.Align.CENTER);
        titleStyle.setAntiAlias(true);

        descriptionStyle = new Paint();
        descriptionStyle.setColor(Color.DKGRAY);
        descriptionStyle.setTextSize(captionFontSize);
        descriptionStyle.setTypeface(Typeface.createFromAsset(view.context.getAssets(), "fonts/Roboto-Regular.ttf"));
        descriptionStyle.setAntiAlias(true);

        amountStyle = new Paint();
        amountStyle.setColor(Color.DKGRAY);
        amountStyle.setTextSize(captionFontSize);
        amountStyle.setTypeface(Typeface.createFromAsset(view.context.getAssets(), "fonts/Roboto-Light.ttf"));
        amountStyle.setAntiAlias(true);

        backgroundStyle = new Paint();
        backgroundStyle.setColor(Color.HSVToColor(new float[]{220f, 0.1f, .90f}));
        backgroundStyle.setAlpha(100);
        backgroundStyle.setStrokeWidth(1f);

        overlayStyle = new Paint();
        overlayStyle.setColor(Color.WHITE);
        overlayStyle.setStyle(Paint.Style.FILL);
        overlayStyle.setAlpha(200);

        overlayBorderStyle = new Paint();
        overlayBorderStyle.setColor(Color.LTGRAY);
        overlayBorderStyle.setAlpha(50);
        overlayBorderStyle.setStrokeWidth(5f);
        overlayBorderStyle.setStyle(Paint.Style.STROKE);
    }

    public void resize(float width, float height){
        frameWidth = width;
        frameHeight = height;
        //descriptionStyle.setTextSize(rect.width() / 24);
        //amountStyle.setTextSize(rect.width() / 24);
        //titleStyle.setTextSize(rect.width() / 24);
    }

    public void draw(Canvas canvas, Bar selectedBar, ChartType chartType){

        int captionRow = 1;
        float margin = 16;
        final String descriptionFormat = "%s";
        final String amountFormat = " %10.2f";
        float captionFontSize = descriptionStyle.getTextSize();

        RectF rect = new RectF();

        class TransactionRow{
            String desc;
            String amount;
            float width;

            public TransactionRow(Transaction transaction){
                desc = transaction.description;
                amount = String.format(amountFormat, transaction.amount);
                width = descriptionStyle.measureText(desc) + 30 + amountStyle.measureText(amount);
            }
        }

        List<TransactionRow> transactionRows = new LinkedList<TransactionRow>();
        float maxWidth = 0;

        if (chartType == ChartType.STACKED || chartType == ChartType.STACKED_DATE){
            for (Transaction t : selectedBar.dayTransactions) {
                TransactionRow tr = new TransactionRow(t);
                transactionRows.add(tr);
                maxWidth = Math.max(maxWidth, tr.width);
            }
        }

        if (chartType == ChartType.GROUPED || chartType == ChartType.GROUPED_DATE){
            TransactionRow tr = new TransactionRow(selectedBar.transaction);
            transactionRows.add(tr);
            maxWidth = Math.max(maxWidth, tr.width);
        }

        rect.left = canvas.getWidth() - maxWidth;
        rect.right = canvas.getWidth();
        rect.top = 0;
        rect.bottom = (3 + transactionRows.size()) * captionRow * 1.5f * captionFontSize;

        canvas.drawRoundRect(new RectF(
                rect.left, rect.top,
                rect.right, rect.bottom),
                10f, 10f, overlayStyle);

        canvas.drawRoundRect(new RectF(
                rect.left + 2, rect.top + 2,
                rect.right - 2, rect.bottom - 2),
                10f, 10f, overlayBorderStyle);
         canvas.drawText(String.format("- %tF -", selectedBar.date.toDate()),
                rect.left + rect.width() / 2.0f, captionRow * 2 * captionFontSize + rect.top, titleStyle);

        captionRow += 2;

        for (TransactionRow t : transactionRows) {
            canvas.drawText(String.format(descriptionFormat, t.desc),
                    rect.left + margin, captionRow * 1.5f * captionFontSize + rect.top,
                    descriptionStyle);
            canvas.drawText(t.amount,
                    rect.right - margin - amountStyle.measureText(t.amount),
                    captionRow * 1.5f * captionFontSize + rect.top,
                    amountStyle);
            captionRow++;
        }
        }


}
