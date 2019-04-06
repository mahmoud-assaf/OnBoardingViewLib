package com.mahmoud.onboardingview

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface.BOLD
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.boardingview_item.*
import java.util.*

class OnBoardingView : FrameLayout {

    private lateinit var pager: ViewPager
    private lateinit var skipBtn: TextView

    private lateinit var appIcon: ImageView
    private  var appIconResId: Drawable?=null
    private var animShake:Animation?=null

    private lateinit var indicator: TextView
    private lateinit var indicatorString: String
    private lateinit var indicatorSpannableString: SpannableString
    private lateinit var screenFragments: ArrayList<BoardingScreenFragment>

    private var screensCount = 0
    private lateinit var mscreens: ArrayList<OnBoardingScreen>
    private lateinit var adapter: OnBoardingViewPagerAdapter
    private lateinit var bar: ProgressBar
    private var barStep = 0

    //attrs
    private  var skipBtnStartText: String?="Skip"
    private  var skipBtnFinishText: String?="Done"

    private var showSeparator=true
    private var foregroundColor=Color.WHITE

    private var indicatorSelectedColor=Color.DKGRAY
    private var indicatorUnSelectedColor=Color.LTGRAY

private var endAction:(()->Unit)?=null
private var finishAction:(()->Boolean)?=null
private var shouldRepeatFinishAction=true

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        LayoutInflater.from(context).inflate(R.layout.onboarding_view, this, true)

        //get attrs
        var styleAttributesArray = context.obtainStyledAttributes(attrs, R.styleable.OnBoardingView)
        skipBtnStartText=if(styleAttributesArray.getString(R.styleable.OnBoardingView_skipBbuttonStartText)==null) skipBtnStartText else styleAttributesArray.getString(R.styleable.OnBoardingView_skipBbuttonStartText)
        skipBtnFinishText=if(styleAttributesArray.getString(R.styleable.OnBoardingView_skipButtonFinishText)==null) skipBtnFinishText else styleAttributesArray.getString(R.styleable.OnBoardingView_skipButtonFinishText)
        showSeparator=styleAttributesArray.getBoolean(R.styleable.OnBoardingView_showSeparator,true)
        foregroundColor=styleAttributesArray.getInt(R.styleable.OnBoardingView_foregroundColor,Color.WHITE)
        indicatorSelectedColor=styleAttributesArray.getInt(R.styleable.OnBoardingView_indicatorSelectedColor,indicatorSelectedColor)
        indicatorUnSelectedColor=styleAttributesArray.getInt(R.styleable.OnBoardingView_indicatorUnselectedColor,indicatorUnSelectedColor)
        appIconResId=styleAttributesArray.getDrawable(R.styleable.OnBoardingView_appIconRes)


        pager = findViewById(R.id.pager)
        skipBtn = findViewById(R.id.skip_btn)
        skipBtn.setTextColor(foregroundColor)
        skipBtn.text=skipBtnStartText
        bar = findViewById(R.id.bar)
        bar.progressDrawable.setColorFilter(indicatorSelectedColor, PorterDuff.Mode.SRC_IN)
        bar.visibility= if (showSeparator) View.VISIBLE else View.INVISIBLE
        appIcon=findViewById(R.id.appIconImage)
        appIconResId?.let {
            appIcon.setImageDrawable(appIconResId)
             animShake = AnimationUtils.loadAnimation(context, R.anim.shake_animation)
            appIcon.animation=animShake
           // appIcon.startAnimation(animShake)
        }
        skipBtn.setOnClickListener {
            //should end onboarding
            //Log.e("skip", "should end")

            endAction?.let {
                it.invoke()
            }
        }


        indicator = findViewById(R.id.indicator)
        pager = findViewById(R.id.pager)


    }


    fun setScreens(screenOns: ArrayList<OnBoardingScreen>) {
        mscreens = screenOns
        screensCount = screenOns.size
        var sb = StringBuilder()
        for (i in 0 until screenOns.size) {
            sb.append("\u25CF ")

        }
        indicatorString = sb.toString()
        indicatorSpannableString = SpannableString(indicatorString)

        setIndicator(0)
        barStep = 100 / screensCount
        bar.progress = barStep

        screenFragments = ArrayList()
        var sfragment: BoardingScreenFragment
        for (i in 0 until screensCount) {
            sfragment = BoardingScreenFragment.newInstance(screenOns.get(i),foregroundColor)
            screenFragments.add(sfragment)
        }
        adapter = OnBoardingViewPagerAdapter((this.context as AppCompatActivity).supportFragmentManager)
        adapter.addFragments(screenFragments)
        pager.adapter = adapter

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                var nextPos :Int
                nextPos = if (position == mscreens.size - 1) mscreens.size - 1 else position + 1
                var color = ArgbEvaluator().evaluate(
                    positionOffset,
                    mscreens.get(position).screenBGColor,
                    mscreens.get(nextPos).screenBGColor
                ) as Int
                pager.setBackgroundColor(color)

            }

            override fun onPageSelected(position: Int) {
                setIndicator(position)
                bar.progress = (position + 1) * barStep
                if (position==screensCount-1)  skipBtn.text=skipBtnFinishText else   skipBtn.text=skipBtnStartText
                appIconResId?.let {
                    appIcon.setImageDrawable(appIconResId)
                    appIcon.animation=animShake

                }
               if(position==screensCount-1){
                   finishAction?.let {
                       if (shouldRepeatFinishAction)  shouldRepeatFinishAction= it.invoke()
                   }
               }
                // pager.setBackgroundColor(mscreens.get(position).screenBGColor)
            }
        })

    }

    fun onEnd(onEndAction:()->Unit){
        endAction=onEndAction


    }
fun onFinish(onFinishAction:()->Boolean){
    finishAction=onFinishAction
}


    private fun setIndicator(position: Int) {
        //  Log.e("indicator pos",position.toString())

        indicatorSpannableString.getSpans(0, indicatorSpannableString.length, ForegroundColorSpan::class.java).forEach {
            indicatorSpannableString.removeSpan(it)
        }

        indicatorSpannableString.getSpans(0, indicatorSpannableString.length, StyleSpan::class.java).forEach {
            indicatorSpannableString.removeSpan(it)
        }
        indicator.setTextColor(indicatorUnSelectedColor)

        indicatorSpannableString.setSpan(
            ForegroundColorSpan(indicatorSelectedColor),
            2 * position, 2 * position + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        indicatorSpannableString.setSpan(
            StyleSpan(BOLD),
            position, position + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        indicator.text = indicatorSpannableString

    }

 private inner  class OnBoardingViewPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        var fragments = ArrayList<BoardingScreenFragment>()

        fun addFragments(fragments: ArrayList<BoardingScreenFragment>) {
            this.fragments = fragments
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

    }

  class BoardingScreenFragment : Fragment() {
    private  val ARG_HEADER = "param1"
    private  val ARG_SUBHEADER= "param2"
    private  val ARG_IMAGE_RES= "param3"
    private  val ARG_BG_COLOR= "param4"
    private  val ARG_FG_COLOR= "param5"
    private var header: String? = null
    private var subheader: String? = null
    private var imageres: Int? = 0
    private var bgcolor: Int = Color.LTGRAY
    private var fgcolor: Int = Color.WHITE


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            header = it.getString(ARG_HEADER)
            subheader = it.getString(ARG_SUBHEADER)
            imageres = it.getInt(ARG_IMAGE_RES)
            bgcolor = it.getInt(ARG_BG_COLOR)
            fgcolor = it.getInt(ARG_FG_COLOR)



        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.boardingview_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        boarding_header.text= header
        boarding_header.setTextColor(fgcolor)
        boarding_subheader.text= subheader
        boarding_subheader.setTextColor(fgcolor)
        boarding_image.setImageDrawable(imageres?.let {
            activity?.getDrawable(it)

        })


    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BoardingScreenFragment.
         */
        @JvmStatic
        fun newInstance(screenOn: OnBoardingScreen, forgroundColor: Int=Color.WHITE) =
            BoardingScreenFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HEADER, screenOn.titleText)
                    putString(ARG_SUBHEADER, screenOn.subTitleText)
                    screenOn.drawableResId?.let { putInt(ARG_IMAGE_RES, it) }
                    putInt(ARG_BG_COLOR, screenOn.screenBGColor)
                    putInt(ARG_FG_COLOR,forgroundColor)
                }
            }
    }
}

}

class OnBoardingScreen(
    val titleText: String = "",
    val subTitleText: String = "",
    val drawableResId: Int? = null,
    val screenBGColor: Int = Color.WHITE
)
