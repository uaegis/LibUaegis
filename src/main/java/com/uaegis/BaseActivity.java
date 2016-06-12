package com.uaegis;

/**
 * Created by Lehyu on 2016/5/23.
 */
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.utils.AsyncExecutor;
import com.utils.InputDialog;
import com.utils.MessageListener;

public class BaseActivity extends Activity implements MessageListener {
    public static final Handler ASYNC_THREAD_HANDLER;
    private static final int MSG_HANDLE_ASYNC_COMPLETE = 10102;
    private static final int MSG_HANDLE_ASYNC_PREPARE = 10101;
    private ViewGroup content;
    private TextView titleLabel;

    public static final Handler UI_THREAD_HANDLER = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.obj instanceof HandlerMessage) {
                HandlerMessage handlerMessage = (HandlerMessage)msg.obj;
                if(handlerMessage.listener != null) {
                    handlerMessage.listener.handleMessage(msg.what, handlerMessage.messageData);
                }
            }

        }
    };


    static {
        HandlerThread thread = new HandlerThread(BaseActivity.class.getName());
        thread.start();
        ASYNC_THREAD_HANDLER = new Handler(thread.getLooper());
    }

    public BaseActivity() {
    }


    public <T> void asyncExecute(AsyncExecutor<T> executor) {
        this.postMessage(MSG_HANDLE_ASYNC_PREPARE, null, new AsyncExecuteAction(executor));
    }
    public Message obtainHandlerMessage(int what, Object obj, MessageListener listener) {
        Message msg = new Message();
        HandlerMessage handlerMessage = new HandlerMessage();
        handlerMessage.listener = listener;
        handlerMessage.messageData = obj;
        msg.what = what;
        msg.obj = handlerMessage;
        return msg;
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.requestWindowFeature(1);
        this.setContentView(R.layout.base);
        this.titleLabel = (TextView)this.findViewById(R.id.main_label_title);
        this.content = (ViewGroup)this.findViewById(R.id.main_g_ctr_content);
    }

    public void postMessage(int which, Object obj, long time, MessageListener listener) {
        Message msg = this.obtainHandlerMessage(which, obj, listener);
        if(time > 0L) {
            UI_THREAD_HANDLER.sendMessageDelayed(msg, time);
        } else {
            UI_THREAD_HANDLER.sendMessage(msg);
        }
    }

    public void postMessage(int which, Object obj, MessageListener listener) {
        this.postMessage(which, obj, 0L, listener);
    }

    public void setContent(View view) {
        content.removeAllViews();
        LayoutParams params = new LayoutParams(-1, -1);
        this.content.addView(view, params);
    }

    public void setTitle(int id) {
        this.titleLabel.setText(id);
    }

    public void setTitle(CharSequence name) {
        this.titleLabel.setText(name);
    }

    public void showInputDialog(String title, String msg, String text,
                                String hidedText, android.content.DialogInterface.OnClickListener listener) {
        InputDialog dialog = new InputDialog(this);
        dialog.setTitle(title);
        dialog.setMessage(msg);
        dialog.setText(text);
        dialog.setHint(hidedText);
        dialog.setListener(listener);
        dialog.show();
    }

    public void showMessageDialog(String title, String msg) {
        Builder builder = new Builder(this);
        if(title == null) {
            title = "信息";
        }

        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton("确定", null);
        builder.create().show();
    }

    @Override
    public void handleMessage(int option, Object obj) {

    }


    private class AsyncExecuteAction<T> implements Runnable, MessageListener<T> {
        private AsyncExecutor<T> executor;

        public AsyncExecuteAction(AsyncExecutor<T> executor) {
            this.executor = executor;
        }

        public void handleMessage(int which, T obj) {
            if(which == MSG_HANDLE_ASYNC_PREPARE) {
                this.executor.executePrepare();
                ASYNC_THREAD_HANDLER.post(this);
            } else if(which == MSG_HANDLE_ASYNC_COMPLETE) {
                executor.executeComplete(obj);
            }

        }

        public void run() {
            Object obj = executor.asyncExecute();
            postMessage(MSG_HANDLE_ASYNC_COMPLETE, obj, this);
        }
    }

    private class HandlerMessage {
        public MessageListener listener;
        public Object messageData;

        private HandlerMessage() {
        }
    }
}

