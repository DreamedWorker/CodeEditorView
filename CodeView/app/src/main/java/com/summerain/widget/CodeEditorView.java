package com.summerain.widget;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/* 编辑框
 * class CodeEditorView
 * author summerain
 * 贴吧id fjwj12138
 * date 2018-7-1
 */
public class CodeEditorView extends View
{
	private static final String TAG = "CodeEditorView";
    //默认值
	private static final int DEFAULT_CANVAS_BACKGROUND_COLOR = 0xffffffff;
	private static final int DEFAULT_TEXT_LINENUMBER_COLOR = 0x802b2b2b;
	private static final int DEFAULT_TEXT_SIZE = 36;
	private static final int DEFAULT_CANVAS_CURSOR_WIDTH = 2;

	//
	private Context mContext;//上下文
	private InputMethodManager mInputMethodManager;
	private DrawUtils mDrawUtils;//绘制工具
	private Paint mLineNumberPaint;//行号画笔
	private Paint mTextPaint;//正文画笔
	private Paint mCursorPaint;//光标画笔
	private Runnable mViewMoveFlashRunnable = null;//惯性动画
    private TimerTask mCursorTwinkleTask = new CursorTwinkle();//光标闪烁动画计时器
    private Timer mCursorTwinkleTimer = new Timer();//计时器

	//配色&参数
	private float mTextSize;
	private int mBackgroundColor;
	private int mLineNumberColor = 0x802b2b2b;

	//参数
	private String mText = "";//正文
	private ArrayList<String> mArrayListText;//正文集合
	private boolean mShowLineNumber = true;//是否显示行号
	private boolean mShowCursor = true;//是否显示光标 动态改变
	private boolean isViewRun = false;//控件是否正在显示
	private boolean isTouching = false;//是否正在按着
	private long mTouchTime = 0;//按住的时间
	private int mCacheDrawLine = 3;//缓存行数 默认3
	private float mScrollX = 0;//原点
	private float mScrollY = 0;
	private float mLastScrollX = 0;//上次原点
	private float mLastScrollY = 0;
	private float mTouchX = 0;//手指触摸坐标
	private float mTouchY = 0;
	private float mDownX = 0;//手指按下时坐标
	private float mDownY = 0;
	private float mDistanceX = 0;//点击距离
	private float mDistanceY = 0;
	private float mCursorWidth = 2;
	private int mCursorPosition = 0;//光标所在格数
	private int mCursorInLine = 1;//光标所在行
	private float mTextDistance = 0;//正文偏移值

	public CodeEditorView(Context context)
    {
        this(context, null);
	}

    public CodeEditorView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.mContext = context;
		mInputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		mDrawUtils = new DrawUtils();
		//读取参数
		TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.CodeEditorView);
        mBackgroundColor = mTypedArray.getColor(R.styleable.CodeEditorView_backgroundColor, DEFAULT_CANVAS_BACKGROUND_COLOR);
		mTextSize = mTypedArray.getDimension(R.styleable.CodeEditorView_textSize, DEFAULT_TEXT_SIZE);
		mCursorWidth = mTypedArray.getDimension(R.styleable.CodeEditorView_cursorWidth, DEFAULT_CANVAS_CURSOR_WIDTH);

        //初始化
		initPaint();
		init();
    }

	//初始化画笔
	private void initPaint()
	{
		//行号画笔
		mLineNumberPaint = new Paint();
		mLineNumberPaint.setColor(Color.parseColor("#802b2b2b"));
		mLineNumberPaint.setTextSize(mTextSize);
		mLineNumberPaint.setAntiAlias(true);
        mLineNumberPaint.setTypeface(Typeface.MONOSPACE);

		//正文画笔
		mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTypeface(Typeface.MONOSPACE);

		//光标画笔
		mCursorPaint = new Paint();
        mCursorPaint.setStrokeWidth(mCursorWidth);
        mCursorPaint.setAntiAlias(true);

	}

	//初始化
	private void init()
	{
		setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
		setText(mText);
	}

    //view被创建时
    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
		isViewRun = true;
		//开启光标闪烁
		mCursorTwinkleTimer.scheduleAtFixedRate(mCursorTwinkleTask, 0, 500);
    }

    //view被销毁时
    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
		isViewRun = false;
		//关闭光标闪烁
        mCursorTwinkleTimer.cancel();
	}

	//绘制画布
	@Override
    public void onDraw(Canvas canvas)
    {
		super.onDraw(canvas);
		canvas.drawColor(mBackgroundColor);
		drawLineNumber(canvas);
		drawText(canvas);
		drawCursor(canvas);
    }

	//触摸监听
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		int action = event.getAction();
		mTouchX = event.getX();
		mTouchY = event.getY();

		switch (action)
		{
			case MotionEvent.ACTION_DOWN://按下时
			    //记录时间
				isTouching = true;
                mTouchTime = System.currentTimeMillis();
			    //记录按下时的坐标
				mDownX = mTouchX;
				mDownY = mTouchY;
				break;

			case MotionEvent.ACTION_MOVE://移动时
			    //记录移动距离并更新原点坐标
				mDistanceX = -(mTouchX - mDownX);
                mDistanceY = -(mTouchY - mDownY);
				//如果单个方向移动距离过小，改为0
				//if(Math.abs(mDistanceX) < 10) mDistanceX = 0;
				//if(Math.abs(mDistanceY) < 10) mDistanceY = 0;
				mScrollX = mLastScrollX + mDistanceX;
                mScrollY = mLastScrollY + mDistanceY;
				//检查原点合法性
				checkScrollX();
				checkScrollY();
				invalidate();

				break;

			case MotionEvent.ACTION_UP://抬起时
			    //记录时间
				isTouching = false;
                mTouchTime = System.currentTimeMillis() - mTouchTime;
			    //记录
				mLastScrollX = mScrollX;
				mLastScrollY = mScrollY;
				mDistanceX = -(mTouchX - mDownX);
                mDistanceY = -(mTouchY - mDownY);
				//判断是否为点击
				if (Math.pow(mDistanceX, 2) <= 1.5 && Math.pow(mDistanceY, 2) <= 1.5)
				{
					showInputSoft();
					mDrawUtils.getCursorPositionAndLine(mDownX, mDownY);
				}
				//启动惯性动画
				mViewMoveFlashRunnable = new ViewMoveFlash(mDistanceX, mDistanceY);
				getHandler().post(mViewMoveFlashRunnable);
				break;
		}
		return true;
	}
	
	/*工具方法*/
	//重写方法使view可编辑
    @Override
    public boolean onCheckIsTextEditor()
    {
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs)
    {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        outAttrs.inputType = InputType.TYPE_NULL;
        InputConnection result = new MyInputConnection();
        return result;
	}

	//弹出输入法
	private void showInputSoft()
	{
		mInputMethodManager.showSoftInput(CodeEditorView.this, InputMethodManager.SHOW_FORCED);
	}

	//检查原点X值的有效性
    private void checkScrollX()
    {
        if (mScrollX < 0)
        {
            mScrollX = 0;
        }
    }

	//判断原点Y值的有效性
    private void checkScrollY()
    {
        //计算最大Y值
        float maxHeight = (mTextPaint.descent() - mTextPaint.ascent()) * (mArrayListText.size() - 1) - this.getHeight() / 4;
		//计算方法在行数较少的情况下有可能出现负值，此处做好判断
		if (maxHeight < 0) maxHeight = 0;

        if (mScrollY < 0)
        {
            mScrollY = 0;
        }

        if (mScrollY > maxHeight)
        {
            mScrollY = maxHeight;
        }
    }

	//判断该行光标位置的有效性
    private void checkCursorPosition()
    {
        if (mCursorPosition < 0)
        {
            mCursorPosition = 0;
        }
        if (mCursorPosition > mArrayListText.get(mCursorInLine - 1).toString().length())
        {
            mCursorPosition = mArrayListText.get(mCursorInLine - 1).toString().length();
        }
    }

    //检查光标行数有效性
    private void checkCursorInLine()
    {
        if (mCursorInLine < 1)
        {
            mCursorInLine = 1;
        }
        if (mCursorInLine > mArrayListText.size())
        {
            mCursorInLine = mArrayListText.size(); 
        }
    }

	//点击回车键情景判断
    private void enterHandle()
    {
        //当光标位于开头时
        if (mCursorPosition == 0)
        {
			//新增一行
            mArrayListText.add(mCursorInLine - 1, "");
            mCursorInLine ++;
            checkCursorInLine();
        }

        //当光标位于末尾时
        if (mArrayListText.get(mCursorInLine - 1).length() != 0 && mCursorPosition == mArrayListText.get(mCursorInLine - 1).length())
        {
            mArrayListText.add(mCursorInLine, "");
            mCursorInLine ++;
            mCursorPosition = 0;
            checkCursorInLine();
        }

        //当光标位于中间时
        if (mCursorPosition != 0 && mCursorPosition != mArrayListText.get(mCursorInLine - 1).length())
        {
			//裁剪文本，新增一行
            String lastLineString = mArrayListText.get(mCursorInLine - 1);
            mArrayListText.remove(mCursorInLine - 1);
            mArrayListText.add(mCursorInLine - 1, lastLineString.substring(0, mCursorPosition));
            mArrayListText.add(mCursorInLine, lastLineString.substring(mCursorPosition, lastLineString.length()));
            mCursorPosition = 0;
            mCursorInLine ++;
        }
        invalidate();
    }

    //删除事件情景判断
    private void deleteHandle()
    {
		//光标不处于开头时
        if (mCursorPosition != 0)
        {
            deleteAt();
            mCursorPosition --;
            checkCursorPosition();
        }
        else
        {
			//处于开头且行数不为1时
            if (mCursorInLine != 1)
            {
                //获取当前行与上一行文本
				String inLineStr = mArrayListText.get(mCursorInLine - 1);
                String upLineStr = mArrayListText.get(mCursorInLine - 2);
                //拼接
				mArrayListText.remove(mCursorInLine - 1);
                mArrayListText.remove(mCursorInLine - 2);
                mArrayListText.add(mCursorInLine - 2, upLineStr + inLineStr);
                //更新光标位置
				mCursorInLine --;
                mCursorPosition = upLineStr.length();
                checkCursorInLine();
                checkCursorPosition();
            }
        }
        invalidate();
    }
	
	
	//绘制行号
	private void drawLineNumber(Canvas canvas)
	{
		if (mShowLineNumber)//判断是否需要绘画行号
		{
			//获取高度
			float height = mLineNumberPaint.descent() - mLineNumberPaint.ascent();
			//计算行号最大宽度
			float maxWidth = mLineNumberPaint.measureText(String.valueOf(mArrayListText.size()));
			//计算单个字符长度
			float oneStrWidth = mLineNumberPaint.measureText(String.valueOf(0));
			//计算正文偏移值
			mTextDistance = maxWidth + oneStrWidth;
			//获取行数
			int start = mDrawUtils.getDrawStartLine();
			int end = mDrawUtils.getDrawEndLine();
			for (int i = start;i < end;i++)
			{
				//计算x,y
				float x = maxWidth - mLineNumberPaint.measureText(String.valueOf(i + 1)) - mScrollX;
				float y = height * (i + 1) - mScrollY;
				canvas.drawText(String.valueOf(i + 1), x, y, mLineNumberPaint);
			}
		}
		else
		{
			mTextDistance = 0;
		}
	}

	//绘制光标
    private void drawCursor(Canvas canvas)
    {
        if (mShowCursor)
        {
			//计算高度
            Paint.FontMetrics fontMetrics = mLineNumberPaint.getFontMetrics();
            float height = (fontMetrics.descent - fontMetrics.ascent);
            //计算坐标
			float x = mTextPaint.measureText(mArrayListText.get(mCursorInLine - 1).toString(), 0, mCursorPosition) + mTextDistance - mScrollX;
            float y = (mTextPaint.descent() - mTextPaint.ascent()) * (mCursorInLine - 1) + fontMetrics.ascent - fontMetrics.top - mScrollY;
            canvas.drawLine(x, y, x, y + height, mCursorPaint);
        }
    }

	//绘制正文
	private void drawText(Canvas canvas)
	{
		//获取高度
        float height = mTextPaint.descent() - mTextPaint.ascent();
        //获取行数
        int start = mDrawUtils.getDrawStartLine();
        int end = mDrawUtils.getDrawEndLine();
        for (int i = start;i < end;i++)
        {
            float x = mTextDistance - mScrollX;
            float y = height * (i + 1) - mScrollY;
			canvas.drawText(mArrayListText.get(i), x, y, mTextPaint);
		}
	}

	/*开放的api*/
	//设置背景
	public void setCanvasBackgroundColor(int color)
	{
		this.mBackgroundColor = color;
		invalidate();
	}

	//是否显示行号
	public void showLineNumber(boolean boo)
	{
		this.mShowLineNumber = boo;
		invalidate();
	}

	//设置正文
    public void setText(String text)
    {
        this.mText = text;
        mArrayListText = new ArrayList<String>();
		if (mText == null) mText = "";//判断空值
		String[] mTextList = mText.split("\n");
		for (int i = 0;i < mTextList.length;i++)
		{
			mArrayListText.add(mTextList[i]);
		}
		//参数初始化，避免报错
        mCursorPosition = 0;
        mCursorInLine = 1;
        mScrollX = 0;
        mScrollY = 0;
        invalidate();
    }

    //获取正文
    public String getText()
    {
        String text = "";
        for (int i = 0;i < mArrayListText.size();i++)
        {
            text += mArrayListText.get(i);
			//防止最后一行多出回车
            if (i < (mArrayListText.size() - 1)) text += "\n";
        }
        return text; 
    }

	//在光标后添加文本
    public void appendText(String str)
    {
		//获取文本行数
		String[] list = str.split("\n");
		int position = list.length;
		//拼接
		String result = mArrayListText.get(mCursorInLine - 1).toString().substring(0, mCursorPosition) + str + mArrayListText.get(mCursorInLine - 1).toString().substring(mCursorPosition, mArrayListText.get(mCursorInLine - 1).toString().length());
        mArrayListText.remove(mCursorInLine - 1);
        mArrayListText.add(mCursorInLine - 1, result);
		//更新位置
		mCursorPosition += str.length();
		checkCursorPosition();
        invalidate(); 
    }

	//删除光标前面的一个字符
    public void deleteAt()
    {
        String result = mArrayListText.get(mCursorInLine - 1).toString().substring(0, mCursorPosition - 1) + mArrayListText.get(mCursorInLine - 1).toString().substring(mCursorPosition, mArrayListText.get(mCursorInLine - 1).toString().length());
        mArrayListText.remove(mCursorInLine - 1);
        mArrayListText.add(mCursorInLine - 1, result);
        invalidate();
    }
	
	//计算工具类
	class DrawUtils
	{
		//计算绘制开始行数
		public int getDrawStartLine()
		{
			//基础高度
			float baseHeight = mLineNumberPaint.descent() - mLineNumberPaint.ascent();
			float line = mScrollY / baseHeight - mCacheDrawLine;
			if (line < 0) return 0;
			return (int)line;
		}

		//计算绘制结束行数
		public int getDrawEndLine()
		{
			//基础高度
			float baseHeight = mLineNumberPaint.descent() - mLineNumberPaint.ascent();
			float line = (mScrollY + CodeEditorView.this.getHeight()) / baseHeight + mCacheDrawLine;
			//三元运算符 判断行数是否越界
			return line > mArrayListText.size() ? mArrayListText.size() : (int)line;
		}

		//根据点击获取光标位置
		public void getCursorPositionAndLine(float mClickX, float mClickY)
		{
			//计算原位置
			float textX = mScrollX + mClickX - mTextDistance;
			float textY = mScrollY + mClickY;
			//基础高度
			float textHeight = mTextPaint.descent() - mTextPaint.ascent();
			//判断行数
			mCursorInLine = (int)Math.floor(textY / textHeight);
			checkCursorInLine();
			mCursorPosition = 0;
			//判断位置
			if (textX >= 0)
			{
				for (int i = 0;i < mArrayListText.get(mCursorInLine - 1).length();i++)
				{
					float width = mTextPaint.measureText(mArrayListText.get(mCursorInLine - 1).substring(0, i));
					if (width < textX)
					{
						mCursorPosition = i + 1;
						checkCursorPosition();
					}
				}
			}
			else
			{
				mCursorPosition = 0;
			}
			invalidate();
		}

	}

	//惯性动画
	class ViewMoveFlash implements Runnable
	{
		double vx,vy;

		public ViewMoveFlash(float mDistanceX, float mDistanceY)
		{
			//记录移动距离为速度
			this.vx = mDistanceX;
			this.vy = mDistanceY;
		}

		@Override
		public void run()
		{
			if (Math.pow(vx, 2) > 0.1 && Math.pow(vy, 2) > 0.1 && !isTouching && mTouchTime < 200)
			{
				vx = vx * 0.96521;
				vy = vy * 0.96521;
				mScrollX += vx;
				mScrollY += vy;
				checkScrollX();
				checkScrollY();
				mLastScrollX = mScrollX;
				mLastScrollY = mScrollY;
				invalidate();
				if (getHandler() != null) getHandler().post(this);
			}
		}
	}

	//光标闪烁动画
    class CursorTwinkle extends TimerTask
    {
        @Override
        public void run()
        {
            mShowCursor = !mShowCursor;
            invalidate();
        }     
    }

	//键盘输入监听类
	class MyInputConnection extends BaseInputConnection
    {
        public MyInputConnection()
        {
            super(CodeEditorView.this, true);
		}

        @Override
        public boolean commitText(CharSequence p1, int p2)
        {
			appendText(p1.toString());
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event)
        {
            if (event.getAction() == KeyEvent.ACTION_DOWN) return super.sendKeyEvent(event);
            int keyCode = event.getKeyCode();
            switch (keyCode)
            {
					//删除键
                case KeyEvent.KEYCODE_DEL:
					deleteHandle();
                    break;

					//回车键
                case KeyEvent.KEYCODE_ENTER:
					enterHandle();
                    break;

					//导航键
                case KeyEvent.KEYCODE_DPAD_UP:
                    mCursorInLine --;
                    checkCursorPosition();
                    checkCursorInLine();
                    invalidate();
                    break;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    mCursorInLine ++;
                    checkCursorPosition();
                    checkCursorInLine();
                    invalidate();
                    break;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mCursorPosition ++;
                    checkCursorInLine();
                    checkCursorPosition();
                    invalidate();
                    break;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mCursorPosition --;
                    checkCursorInLine();
                    checkCursorPosition();
                    invalidate();
                    break;  
            }
            return true;
		}
    }

}
