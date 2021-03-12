package io.snabble.sdk.ui.checkout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.snabble.sdk.Checkout
import io.snabble.sdk.Project
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.getFragmentActivity
import kotlinx.android.synthetic.main.snabble_view_checkout_pos.view.*
import kotlinx.android.synthetic.main.snabble_view_payment_status.view.*
import kotlinx.android.synthetic.main.snabble_view_shopping_cart.view.*

@Suppress("LeakingThis")
open class PaymentStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    Checkout.OnCheckoutStateChangedListener,
    Checkout.OnFulfillmentUpdateListener,
    LifecycleObserver {

    private var project: Project
    private var checkout: Checkout
    private var paymentItem: PaymentStatusItemView
    private var fulfillmentItem: PaymentStatusItemView
    private var exitTokenItem: PaymentStatusItemView
    private var receiptItem: PaymentStatusItemView

    init {
        inflate(getContext(), R.layout.snabble_view_payment_status, this)
        project = SnabbleUI.getProject()
        checkout = project.checkout
        paymentItem = findViewById(R.id.payment)
        fulfillmentItem = findViewById(R.id.fulfillment)
        exitTokenItem = findViewById(R.id.exit_token)
        receiptItem = findViewById(R.id.receipt)

        getFragmentActivity()?.lifecycle?.addObserver(this)
        attachListeners()
    }

    override fun onStateChanged(state: Checkout.State?) {
        paymentItem.visibility = View.VISIBLE
        paymentItem.setText("Bezahlung")

        when (state) {
            Checkout.State.PAYMENT_PROCESSING -> {
                paymentItem.state = PaymentStatusItemView.State.IN_PROGRESS
            }
            Checkout.State.PAYMENT_APPROVED -> {
                paymentItem.state = PaymentStatusItemView.State.SUCCESS
            }
            Checkout.State.DENIED_TOO_YOUNG -> paymentItem.state = PaymentStatusItemView.State.FAILED
            Checkout.State.DENIED_BY_PAYMENT_PROVIDER -> paymentItem.state = PaymentStatusItemView.State.FAILED
            Checkout.State.DENIED_BY_SUPERVISOR -> paymentItem.state = PaymentStatusItemView.State.FAILED
            Checkout.State.PAYMENT_ABORTED -> paymentItem.state = PaymentStatusItemView.State.FAILED
            Checkout.State.PAYMENT_ABORT_FAILED -> paymentItem.state = PaymentStatusItemView.State.FAILED
        }
    }

    // TODO entry tokens!

    override fun onFulfillmentUpdated() {

    }

    override fun onFulfillmentDone() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun attachListeners() {
        checkout.addOnCheckoutStateChangedListener(this)
        checkout.addOnFulfillmentListener(this)

        onStateChanged(checkout.state)
        onFulfillmentUpdated()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun detachListeners() {
        checkout.removeOnCheckoutStateChangedListener(this)
        checkout.removeOnFulfillmentListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachListeners()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detachListeners()
    }
}