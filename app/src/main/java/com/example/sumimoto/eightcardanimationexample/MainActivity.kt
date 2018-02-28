package com.example.sumimoto.eightcardanimationexample

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import rx.Observable
import rx.subjects.BehaviorSubject


class MainActivity : AppCompatActivity() {

    private val titleText by lazy { findViewById(R.id.title) as TextView }
    private val buttonContainer by lazy { findViewById(R.id.buttonContainer) as LinearLayout }
    private val nextButton by lazy { findViewById(R.id.nextButton) as Button }
    private val linkButton by lazy { findViewById(R.id.linkContainer) as View }
    private val addButton by lazy { findViewById(R.id.addContainer) as View }


    private val list: List<Boolean> = listOf(false, false, true, false)

    private val cardViews by lazy {
        listOf(
                findViewById(R.id.cardView1) as CardView to findViewById(R.id.cardViewImage1) as ImageView,
                findViewById(R.id.cardView2) as CardView to findViewById(R.id.cardViewImage2) as ImageView,
                findViewById(R.id.cardView3) as CardView to findViewById(R.id.cardViewImage3) as ImageView,
                findViewById(R.id.cardView4) as CardView to findViewById(R.id.cardViewImage4) as ImageView)
    }

    private val fadeViews by lazy {
        listOf(titleText, nextButton, linkButton, addButton)
    }

    private val num = BehaviorSubject.create(0)

    private val cardLongSidePx: Int
        get() {
            windowManager.defaultDisplay.let {
                val point = Point()
                it.getSize(point)
                return (point.x * 0.7).toInt()
            }
        }

    private fun initViews() {
        val cardViewsObsevable = Observable.from(cardViews)

        findViewById(R.id.cardViewImage1).runOnAfterLayout {
            Observable.zip(cardViewsObsevable, cardViewsObsevable.skip(1), { t1, t2 -> t1 to t2 })
                    .subscribe {
                        val cardImageView1 = it.first.second
                        val cardImageView2 = it.second.second
                        val cardView1 = it.first.first
                        val cardView2 = it.second.first
                        cardImageView2.layoutParams = cardImageView2.layoutParams.apply {
                            height = (cardImageView1.height * 0.9f).toInt()
                            width = (cardImageView1.width * 0.9f).toInt()
                            cardView2.translationY = cardView1.translationY + (ViewUtils.dpToPx(this@MainActivity, 16)).toFloat()
                        }
                    }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardViewPairs: List<Pair<CardView, ImageView>> = cardViews
        initViews()

        fab.setOnClickListener {

            val targetPair = cardViewPairs[num.value]
            val targetCardView = targetPair.first

            val location = intArrayOf(0, 0)
            targetCardView.getLocationOnScreen(location)

            val isVertical = list[num.value]
            val transitionYValue = ((location[1] + if (isVertical) targetCardView.width else targetCardView.height) * -1).toFloat()
            val hasNext = list.size - 1 > num.value
            val animators = listOf(
                    createFrontCardTransitionYAnimation(targetCardView, transitionYValue),
                    if (hasNext) createNextCardAnimation(list[num.value + 1], cardViewPairs[num.value + 1].first) else null,
                    if (hasNext) createBackCardsScaleAnimation() else null
            ).filterNotNull()

            val animationSet = AnimatorSet()
            animationSet.playTogether(animators)
            animationSet.duration = 300
            animationSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) = Unit
                override fun onAnimationCancel(animation: Animator?) = Unit

                override fun onAnimationStart(animation: Animator?) {
                    // フェードで隠す
                    fadeViews.forEach { it.animate().alpha(0.0F).setDuration(300).start() }
                }

                override fun onAnimationEnd(animation: Animator?) {
                    // フェードで表示
                    fadeViews.forEach { it.animate().alpha(1.0F).setDuration(300).start() }

                    num.onNext(num.value + 1)
                    if (cardViewPairs.size <= num.value) {
                        num.onNext(0)
                        Toast.makeText(this@MainActivity, "〜終わり〜", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            animationSet.start()
        }
    }

    private fun createBackCardsScaleAnimation(): AnimatorSet {
        val animators = mutableListOf<Animator>()
        (num.value + 1 until 4).forEachIndexed { index, i ->
            val scale = 1f - 10.1f * (index + 1)
            Log.d("@@", "$index, $i, $scale")

            animators.add(createScaleAnimation(cardViews[i].first, 1f - 0.1f * index))
        }
        return AnimatorSet().apply { playTogether(animators) }
    }

    private fun createFrontCardTransitionYAnimation(target: View, transitionYValue: Float) =
            ObjectAnimator.ofFloat(target, "translationY", transitionYValue)

    private fun createNextCardAnimation(needRotate: Boolean, target: View): AnimatorSet {
        val animationSet = AnimatorSet()
        animationSet.duration = 200
        if (needRotate) {
            animationSet.playTogether(
//                    ObjectAnimator.ofFloat(target, "translationY", -30f),
                    createScaleAnimation(target, 1f),
                    ObjectAnimator.ofFloat(target, "rotation", 90f))
        } else {
            animationSet.playTogether(
//                    ObjectAnimator.ofFloat(target, "translationY", -30f),
                    createScaleAnimation(target, 1f))
        }
        return animationSet
    }

    private fun createScaleAnimation(target: View, scale: Float): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(
                    ObjectAnimator.ofFloat(target, "scaleX", scale),
                    ObjectAnimator.ofFloat(target, "scaleY", scale)
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun View.runOnAfterLayout(runnable: (View) -> Unit) {
        this.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                this@runOnAfterLayout.viewTreeObserver.removeOnGlobalLayoutListenerCompat(this)
                runnable(this@runOnAfterLayout)
            }
        })
    }

    fun ViewTreeObserver.removeOnGlobalLayoutListenerCompat(onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener?) =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                @Suppress("DEPRECATION")
                removeGlobalOnLayoutListener(onGlobalLayoutListener)
            } else {
                removeOnGlobalLayoutListener(onGlobalLayoutListener)
            }
}
