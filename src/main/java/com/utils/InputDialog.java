package com.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.uaegis.R;


/**
 * Created by Lehyu on 2016/5/18.
 */
public class InputDialog extends Dialog
        implements View.OnClickListener
{
    public static final String KEY_DESCRIPTION = "dialog.description";
    public static final String KEY_HINT = "dialog.hint";
    public static final String KEY_TEXT = "dialog.text";
    private OnClickListener listener;
    private TextView messageLabel;
    private Button negativeButton;
    private Button positiveButton;
    private EditText text;
    private TextView titleLabel;

    public InputDialog(Context paramContext)
    {
        super(paramContext);
        init();
    }

    public InputDialog(Context paramContext, int paramInt)
    {
        super(paramContext, paramInt);
        init();
    }

    public String getText()
    {
        return this.text.getText().toString().trim();
    }

    protected void init()
    {
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        setCancelable(true);
        setCanceledOnTouchOutside(false);
        requestWindowFeature(1);
        setContentView(R.layout.uaegis_input_dialog);
        this.titleLabel = ((TextView)findViewById(R.id.c_label_dialog_title));
        this.messageLabel = ((TextView)findViewById(R.id.c_label_dialog_msg));
        this.text = ((EditText)findViewById(R.id.uaegis_c_text_dialog_input));
        this.positiveButton = ((Button)findViewById(R.id.uaegis_c_btn_dialog_ok));
        this.positiveButton.setOnClickListener(this);
        this.negativeButton = ((Button)findViewById(R.id.uaegis_c_btn_dialog_cancel));
        this.negativeButton.setOnClickListener(this);
    }

    public void onClick(View paramView)
    {
        if (paramView.getId() == R.id.uaegis_c_btn_dialog_ok)
        {
            dismiss();
            onDialogClose(-1);
        }
        while (paramView.getId() != R.id.uaegis_c_btn_dialog_cancel)
            return;
        dismiss();
        onDialogClose(-2);
    }

    protected void onDialogClose(int paramInt)
    {
        if (this.listener != null)
            this.listener.onClick(this, paramInt);
    }

    public void setButtonText(String paramString1, String paramString2)
    {
        if (paramString1 == null)
            paramString1 = "确定";
        if (paramString2 == null)
            paramString2 = "取消";
        this.positiveButton.setText(paramString1);
        this.negativeButton.setText(paramString2);
    }

    public void setHint(String paramString)
    {
        this.text.setHint(paramString);
    }

    public void setListener(OnClickListener paramOnClickListener)
    {
        this.listener = paramOnClickListener;
    }

    public void setMessage(int paramInt)
    {
        this.messageLabel.setText(paramInt);
    }

    public void setMessage(CharSequence paramCharSequence)
    {
        this.messageLabel.setText(paramCharSequence);
    }

    public void setText(String paramString)
    {
        this.text.setText(paramString);
    }

    public void setTitle(int paramInt)
    {
        this.titleLabel.setText(paramInt);
    }

    public void setTitle(CharSequence paramCharSequence)
    {
        this.titleLabel.setText(paramCharSequence);
    }
}