package com.example.sumimoto.eightcardanimationexample

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import rx.subjects.BehaviorSubject


class MainActivity : AppCompatActivity() {

    private val titleText by lazy { findViewById(R.id.title) as TextView }
    private val buttonContainer by lazy { findViewById(R.id.buttonContainer) as LinearLayout }
    private val nextButton by lazy { findViewById(R.id.nextButton) as Button }
    private val linkButton by lazy { findViewById(R.id.linkContainer) as View }
    private val addButton by lazy { findViewById(R.id.addContainer) as View }

    private val cardViewContainer by lazy { findViewById(R.id.cardViewContainer) as ConstraintLayout }

    private val list: List<Boolean> = listOf(false, false, true, false)

    private val cardImageViews by lazy {
        listOf(
                findViewById(R.id.cardViewImage1) as ImageView,
                findViewById(R.id.cardViewImage2) as ImageView,
                findViewById(R.id.cardViewImage3) as ImageView,
                findViewById(R.id.cardViewImage4) as ImageView)
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
                return point.x - ViewUtils.dpToPx(this@MainActivity, 52) * 2
            }
        }

    private val cardImageViewTopMargin by lazy { ViewUtils.dpToPx(this@MainActivity, 8) }

    private fun initViews() {
        findViewById(R.id.cardViewContainer).runOnAfterLayout { container ->
            cardImageViews.forEachIndexed { index, imageView ->
                val scale = Math.pow(0.9, index.toDouble())
                val ratio = imageView.height.toFloat() / imageView.width.toFloat()

                val constraintSet = ConstraintSet()
                constraintSet.clone(cardViewContainer)
                constraintSet.constrainWidth(imageView.id, (container.width * scale).toInt())
                constraintSet.constrainHeight(imageView.id, (container.height * scale * ratio).toInt())
                constraintSet.applyTo(cardViewContainer)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        nextButton.setOnClickListener { animateViews() }
        addButton.setOnClickListener { animateViews() }
    }

    private fun animateViews() {
        val imageView = cardImageViews[num.value]

        val location = intArrayOf(0, 0)
        imageView.getLocationOnScreen(location)
        val displayWidth = location[1]

        val currentCardIndex = num.value
        val nextCardIndex = num.value + 1

        val isVertical = list[currentCardIndex]
        val transitionYValue = ((displayWidth + if (isVertical) imageView.width else imageView.height) * -1).toFloat()
        val hasNext = list.size - 1 > currentCardIndex
        val animators = listOfNotNull(
                createFrontCardTransitionYAnimation(imageView, transitionYValue),
                if (hasNext) createNextCardAnimation(nextCardIndex, list[nextCardIndex], cardImageViews[nextCardIndex]) else null,
                if (hasNext) createBackCardsScaleAnimation() else null)

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

                num.onNext(nextCardIndex)
                if (cardImageViews.size <= num.value) {
                    num.onNext(0)
                    Toast.makeText(this@MainActivity, "〜終わり〜", Toast.LENGTH_SHORT).show()
                }
            }
        })
        animationSet.start()
    }

    private fun createBackCardsScaleAnimation(): AnimatorSet {
        val animators = mutableListOf<Animator>()
        (num.value + 1 until 4).forEachIndexed { index, i ->
            animators.add(createScaleAnimation(index, cardImageViews[i]))
        }
        return AnimatorSet().apply { playTogether(animators) }
    }

    private fun createFrontCardTransitionYAnimation(target: View, transitionYValue: Float) =
            ObjectAnimator.ofFloat(target, "translationY", transitionYValue)

    private fun createNextCardAnimation(index: Int, needRotate: Boolean, target: View): AnimatorSet {
        val animationSet = AnimatorSet()
        animationSet.duration = 200
        if (needRotate) {
            animationSet.playTogether(
                    ObjectAnimator.ofFloat(target, "translationY", (cardImageViewTopMargin * -1).toFloat()),
                    createScaleAnimation(index, target),
                    ObjectAnimator.ofFloat(target, "rotation", 90f))
        } else {
            animationSet.playTogether(
                    ObjectAnimator.ofFloat(target, "translationY", (cardImageViewTopMargin * -1).toFloat()),
                    createScaleAnimation(index, target))
        }
        return animationSet
    }

    private fun createScaleAnimation(index: Int, target: View): AnimatorSet {
        return AnimatorSet().apply {
            val scale = Math.pow(0.9, index.toDouble())
            val ratio = target.height.toFloat() / target.width.toFloat()

            playTogether(
                    ValueAnimator.ofInt(target.width, (cardLongSidePx * scale).toInt()).apply {
                        addUpdateListener { valueAnimator ->
                            target.layoutParams = target.layoutParams.apply {
                                width = valueAnimator.animatedValue as Int
                            }
                        }
                    },
                    ValueAnimator.ofInt(target.height, (cardLongSidePx * ratio * scale).toInt()).apply {
                        addUpdateListener { valueAnimator ->
                            target.layoutParams = target.layoutParams.apply {
                                height = valueAnimator.animatedValue as Int
                            }
                        }
                    }
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
