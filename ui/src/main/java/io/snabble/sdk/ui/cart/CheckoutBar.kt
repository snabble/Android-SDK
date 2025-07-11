package io.snabble.sdk.ui.cart

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.button.PayButton
import io.snabble.accessibility.accessibility
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.checkout.CheckoutState
import io.snabble.sdk.checkout.DepositReturnVoucher
import io.snabble.sdk.checkout.DepositReturnVoucherState
import io.snabble.sdk.config.ExternalBillingSubjectLength
import io.snabble.sdk.config.ProjectId
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.checkout.CheckoutActivity
import io.snabble.sdk.ui.payment.PaymentInputViewHelper
import io.snabble.sdk.ui.payment.SEPALegalInfoHelper
import io.snabble.sdk.ui.payment.SelectPaymentMethodFragment
import io.snabble.sdk.ui.payment.externalbilling.ui.widgets.SubjectAlertDialog
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.DelayedProgressDialog
import io.snabble.sdk.ui.utils.I18nUtils
import io.snabble.sdk.ui.utils.OneShotClickListener
import io.snabble.sdk.ui.utils.SnackbarUtils
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.executeUiAction
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.ui.utils.requireFragmentActivity
import io.snabble.sdk.ui.utils.setOneShotClickListener
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.getColorByAttribute

open class CheckoutBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        inflate(getContext(), R.layout.snabble_view_checkout_bar, this)
    }

    private val paymentSelectorButton = findViewById<View>(R.id.payment_selector_button)
    private val paymentSelectorButtonBig = findViewById<View>(R.id.payment_selector_button_big)
    private val payButton = findViewById<Button>(R.id.pay)
    private val priceSum = findViewById<TextView>(R.id.price_sum)
    private val sumContainer = findViewById<View>(R.id.sum_container)
    private val googlePayButtonLayout = findViewById<PayButton>(R.id.google_pay_button_layout)
    private val paymentSelector = findViewById<View>(R.id.payment_selector)
    private val paymentIcon = findViewById<ImageView>(R.id.payment_icon)
    private val paymentActive = findViewById<View>(R.id.payment_active)
    private val articleCount = findViewById<TextView>(R.id.article_count)

    private lateinit var progressDialog: DelayedProgressDialog

    private val paymentSelectionHelper by lazy { PaymentSelectionHelper.getInstance() }
    private lateinit var project: Project
    private val cart: ShoppingCart by lazy { project.shoppingCart }
    private val cartChangeListener = object : SimpleShoppingCartListener() {
        override fun onChanged(cart: ShoppingCart) = update()
    }

    val priceHeight: Int
        get() = priceSum.height + sumContainer.marginTop * 2

    var checkoutPreconditionHandler: CheckoutPreconditionHandler? = null

    init {
        orientation = VERTICAL

        if (!isInEditMode) {
            val currentProject = instance.checkedInProject.getValue()
            currentProject?.let {
                initBusinessLogic(it)
            }

            Snabble.checkedInProject.value?.let {
                initBusinessLogic(it)
            }
        }
    }

    private fun initBusinessLogic(project: Project) {
        if (this::project.isInitialized && this.project == project) {
            return
        }

        this.project = project

        paymentSelectionHelper.selectedEntry.observe(UIUtils.getHostActivity(context) as FragmentActivity) {
            update()
        }

        paymentSelectorButton.setOnClickListener {
            paymentSelectionHelper.showDialog(UIUtils.getHostFragmentActivity(context))
        }

        paymentSelectorButtonBig.setOnClickListener {
            paymentSelectionHelper.showDialog(UIUtils.getHostFragmentActivity(context))
        }

        payButton.setOneShotClickListener {
            cart.taxation = Taxation.UNDECIDED
            handleButtonClick()
        }

        project.googlePayHelper?.allowedPaymentMethods?.let { allowedPaymentMethods ->
            googlePayButtonLayout.initialize(
                ButtonOptions.newBuilder().setAllowedPaymentMethods(allowedPaymentMethods.toString()).build()
            )
        }
        googlePayButtonLayout.setOneShotClickListener { handleButtonClick() }

        cart.addListener(cartChangeListener)
        update()

        progressDialog = DelayedProgressDialog(context)
        progressDialog.setMessage(context.getString(R.string.Snabble_pleaseWait))
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        progressDialog.setOnKeyListener(DialogInterface.OnKeyListener { _, _, keyEvent: KeyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                project.checkout.abort()
                return@OnKeyListener true
            }
            false
        })

        project.checkout.state.observeView(this, ::onStateChanged)
    }

    private fun handleButtonClick() {
        if (cart.isRestorable) {
            cart.restore()
            update()
        } else {
            onPayClick()
        }
    }

    protected open fun onPayClick() {
        if (checkoutPreconditionHandler?.isCheckoutAllowed() != false) {
            pay()
        }
    }

    private fun update() {
        updatePaymentSelector()
        updatePayAndText()
    }

    private fun updatePaymentSelector() {
        val entry = paymentSelectionHelper.selectedEntry.value
        if (entry == null) {
            paymentSelector.visibility = GONE
        } else {
            val pcs = Snabble.paymentCredentialsStore
            val hasNoPaymentMethods = pcs.usablePaymentCredentialsCount == 0
            val isHidden = project.paymentMethodDescriptors.size == 1 && hasNoPaymentMethods
            paymentSelector.isVisible = !isHidden
            paymentIcon.setImageResource(entry.iconResId)
            paymentSelectorButton.contentDescription =
                resources.getString(R.string.Snabble_Shoppingcart_Accessibility_paymentMethod, entry.text)
            paymentSelectorButton.accessibility {
                setClickAction(R.string.Snabble_Shoppingcart_BuyProducts_selectPaymentMethod)
            }
        }
    }

    private fun updatePayAndText() {
        cart.let { cart ->
            val quantity = cart.totalQuantity
            val price = cart.totalPrice
            val articlesText = resources.getQuantityText(R.plurals.Snabble_Shoppingcart_numberOfItems, quantity)
            articleCount.text = String.format(articlesText.toString(), quantity)
            priceSum.text = project.priceFormatter.format(price)
            priceSum.setTextColor(
                if (price < 0) {
                    context.getColorByAttribute(R.attr.colorError)
                } else {
                    context.getColorByAttribute(R.attr.colorOnSurface)
                }
            )

            val onlinePaymentAvailable = !cart.availablePaymentMethods.isNullOrEmpty()
            payButton.isEnabled =
                price > 0 && (onlinePaymentAvailable || paymentSelectionHelper.selectedEntry.value != null)

            var showBigSelector = paymentSelectionHelper.shouldShowBigSelector()
            val showSmallSelector = paymentSelectionHelper.shouldShowSmallSelector()

            if (paymentSelectionHelper.shouldShowPayButton()) {
                payButton.isEnabled = true
                if (paymentSelectionHelper.shouldShowGooglePayButton()) {
                    showBigSelector = false
                    payButton.isVisible = false
                    googlePayButtonLayout.isVisible = true
                } else {
                    payButton.isVisible = !showBigSelector
                    googlePayButtonLayout.isVisible = false
                }
            } else {
                payButton.isVisible = true
                payButton.isEnabled = false
                googlePayButtonLayout.isVisible = false
            }

            paymentSelectorButtonBig.isVisible = showBigSelector
            paymentSelector.isVisible = price >= 0 && showSmallSelector
            paymentActive.isVisible = !showBigSelector

            if (cart.isRestorable) {
                payButton.isEnabled = true
                payButton.setText(R.string.Snabble_Shoppingcart_EmptyState_restoreButtonTitle)
            } else {
                if (price == 0 && !cart.isEmpty) {
                    payButton.setText(
                        I18nUtils.getIdentifierForProject(
                            resources,
                            project,
                            R.string.Snabble_Shoppingcart_completePurchase
                        )
                    )
                } else {
                    payButton.setText(
                        I18nUtils.getIdentifierForProject(
                            resources,
                            project,
                            R.string.Snabble_Shoppingcart_BuyProducts_now
                        )
                    )
                }
            }
        }
    }

    protected fun pay() {
        if (cart.hasReachedMaxCheckoutLimit()) {
            val message = resources.getString(
                R.string.Snabble_LimitsAlert_checkoutNotAvailable,
                project.priceFormatter.format(project.maxCheckoutLimit)
            )
            SnackbarUtils.make(this, message, UIUtils.SNACKBAR_LENGTH_VERY_LONG).show()
        } else {
            val entry = paymentSelectionHelper.selectedEntry.value
            if (entry != null) {
                if (entry.paymentMethod.isRequiringCredentials && entry.paymentCredentials == null) {
                    PaymentInputViewHelper.openPaymentInputView(context, entry.paymentMethod, project.id)
                } else {
                    Telemetry.event(Telemetry.Event.ClickCheckout)
                    SEPALegalInfoHelper.showSEPALegalInfoIfNeeded(
                        context,
                        entry.paymentMethod,
                        object : OneShotClickListener() {
                            override fun click() {
                                if (entry.paymentMethod.isOfflineMethod) {
                                    project.checkout.checkout(3000, true)
                                } else {
                                    project.checkout.checkout()
                                }
                            }
                        })
                }
            } else {
                val hasPaymentMethodThatRequiresCredentials =
                    project.paymentMethodDescriptors.any { descriptor ->
                        descriptor.paymentMethod?.isRequiringCredentials == true
                    }
                if (hasPaymentMethodThatRequiresCredentials) {
                    val activity = UIUtils.getHostActivity(context)
                    if (activity is FragmentActivity) {
                        val dialogFragment = SelectPaymentMethodFragment()
                        val bundle = Bundle()
                        bundle.putString(
                            SelectPaymentMethodFragment.ARG_PROJECT_ID,
                            requireNotNull(Snabble.checkedInProject.value).id
                        )
                        dialogFragment.arguments = bundle
                        dialogFragment.show(activity.supportFragmentManager, null)
                    }
                } else {
                    Toast.makeText(context, R.string.Snabble_Payment_errorStarting, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    protected fun onStateChanged(state: CheckoutState) {
        when (state) {
            CheckoutState.HANDSHAKING -> {
                progressDialog.showAfterDelay(300)
            }

            CheckoutState.REQUEST_PAYMENT_METHOD -> {
                val entry = paymentSelectionHelper.selectedEntry.value
                if (entry == null) {
                    progressDialog.dismiss()
                    Toast.makeText(context, R.string.Snabble_Payment_errorStarting, Toast.LENGTH_LONG).show()
                    return
                }
                if (entry.paymentCredentials != null) {
                    progressDialog.dismiss()
                    when (entry.paymentMethod) {
                        PaymentMethod.TEGUT_EMPLOYEE_CARD -> {
                            project.checkout.pay(entry.paymentMethod, entry.paymentCredentials)
                        }
                        PaymentMethod.EXTERNAL_BILLING -> {
                            SubjectAlertDialog(context, maxSubjectLength = getMaxSubjectLength())
                                .addMessageClickListener { message ->
                                    entry.paymentCredentials.additionalData["subject"] = message
                                    project.checkout.pay(entry.paymentMethod, entry.paymentCredentials)
                                }
                                .addSkipClickListener {
                                    project.checkout.pay(
                                        entry.paymentMethod,
                                        entry.paymentCredentials
                                    )
                                }
                                .setOnCanceledListener {
                                    project.checkout.abort()
                                }
                                .show()
                        }
                        else -> {
                            Keyguard.unlock(UIUtils.getHostFragmentActivity(context), object : Keyguard.Callback {
                                override fun success() {
                                    progressDialog.showAfterDelay(300)
                                    project.checkout.pay(entry.paymentMethod, entry.paymentCredentials)
                                }

                                override fun error() {
                                    progressDialog.dismiss()
                                    project.checkout.reset()
                                }
                            })
                        }
                    }
                } else {
                    progressDialog.showAfterDelay(300)
                    project.checkout.pay(entry.paymentMethod, null)
                }
            }

            CheckoutState.REQUEST_PAYMENT_AUTHORIZATION_TOKEN -> {
                val price = project.checkout.verifiedOnlinePrice
                if (price != Checkout.INVALID_PRICE) {
                    val googlePayHelper = project.googlePayHelper
                    if (googlePayHelper != null) {
                        googlePayHelper.requestPayment(price)
                    } else {
                        project.checkout.abort()
                    }
                } else {
                    project.checkout.abort()
                }
            }

            CheckoutState.WAIT_FOR_GATEKEEPER,
            CheckoutState.WAIT_FOR_SUPERVISOR,
            CheckoutState.WAIT_FOR_APPROVAL,
            CheckoutState.PAYMENT_APPROVED,
            CheckoutState.PAYMENT_TRANSFERRED,
            CheckoutState.DENIED_BY_PAYMENT_PROVIDER,
            CheckoutState.DENIED_BY_SUPERVISOR,
            CheckoutState.PAYMENT_PROCESSING -> {
                executeUiAction(SnabbleUI.Event.SHOW_CHECKOUT, Bundle().apply {
                    putString(CheckoutActivity.ARG_PROJECT_ID, project.id)
                })
                progressDialog.dismiss()
            }

            CheckoutState.INVALID_PRODUCTS -> {
                val invalidProducts = project.checkout.invalidProducts
                if (!invalidProducts.isNullOrEmpty()) {
                    val res = resources
                    val sb = StringBuilder()
                    if (invalidProducts.size == 1) {
                        sb.append(I18nUtils.getIdentifier(res, R.string.Snabble_SaleStop_ErrorMsg_one))
                    } else {
                        sb.append(I18nUtils.getIdentifier(res, R.string.Snabble_SaleStop_errorMsg))
                    }
                    sb.append("\n\n")
                    for (product in invalidProducts) {
                        if (product.subtitle != null) {
                            sb.append(product.subtitle)
                            sb.append(" ")
                        }
                        sb.append(product.name)
                        sb.append("\n")
                    }
                    AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle(I18nUtils.getIdentifier(resources, R.string.Snabble_SaleStop_ErrorMsg_title))
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.Snabble_ok, null)
                        .show()
                } else {
                    SnackbarUtils.make(this, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show()
                }
                progressDialog.dismiss()
            }

            CheckoutState.INVALID_ITEMS -> {
                val invalidItems = project.checkout.invalidItems

                if (!invalidItems.isNullOrEmpty()) {
                    context.showInvalidProductsDialog(
                        invalidItems = invalidItems,
                        onRemove = {
                            invalidItems.forEach {
                                val index = cart.indexOf(it)
                                if (index != -1) {
                                    cart.remove(index)
                                }
                            }
                        }
                    )
                } else {
                    SnackbarUtils.make(this, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show()
                }
                progressDialog.dismiss()
            }

            CheckoutState.CONNECTION_ERROR,
            CheckoutState.NO_SHOP,
            CheckoutState.PAYMENT_PROCESSING_ERROR -> {
                if (isAttachedToWindow) {
                    SnackbarUtils.make(this, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .show()
                    progressDialog.dismiss()
                }
            }

            CheckoutState.DEPOSIT_RETURN_REDEMPTION_FAILED -> {
                progressDialog.dismiss()
                handleFailedDepositReturns()
            }

            CheckoutState.PAYMENT_ABORTED -> {
                progressDialog.dismiss()
            }

            CheckoutState.REQUEST_VERIFY_AGE -> {
                SnabbleUI.executeAction(requireFragmentActivity(), SnabbleUI.Event.SHOW_AGE_VERIFICATION)
                progressDialog.dismiss()
            }

            CheckoutState.REQUEST_TAXATION -> {
                progressDialog.dismiss()
                AlertDialog.Builder(context)
                    .setTitle(I18nUtils.getIdentifier(context.resources, R.string.Snabble_Taxation_consumeWhere))
                    .setAdapter(
                        ArrayAdapter(
                            context, R.layout.snabble_item_taxation, listOf(
                                context.getString(R.string.Snabble_Taxation_Consume_inhouse),
                                context.getString(R.string.Snabble_Taxation_Consume_takeaway)
                            )
                        )
                    ) { dialog, which ->
                        if (which == 0) {
                            cart.taxation = Taxation.IN_HOUSE
                        } else {
                            cart.taxation = Taxation.TAKEAWAY
                        }
                        dialog.dismiss()
                        project.checkout.checkout()
                    }
                    .create()
                    .show()
            }

            CheckoutState.NO_PAYMENT_METHOD_AVAILABLE -> {
                AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(I18nUtils.getIdentifier(resources, R.string.Snabble_SaleStop_ErrorMsg_title))
                    .setMessage(I18nUtils.getIdentifier(resources, R.string.Snabble_Payment_noMethodAvailable))
                    .setPositiveButton(R.string.Snabble_ok, null)
                    .show()
                progressDialog.dismiss()
            }

            else -> {
                Logger.d("Unhandled event in CheckoutBar: $state")
            }
        }
    }

    private fun handleFailedDepositReturns() {
        val failedDepositReturnVouchers =
            project.checkout.checkoutProcess?.depositReturnVouchers
                ?.filter { it.state == DepositReturnVoucherState.REDEEMING_FAILED }
                ?: return

        val message = failedDepositReturnVouchers
            .mapNotNull(::getDisplayName)
            .joinToString(separator = "/n") { it }

        AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(context.getString(R.string.Snabble_ShoppingCart_DepositVoucher_RedemptionFailed_title))
            .setMessage(
                resources.getQuantityString(
                    R.plurals.Snabble_ShoppingCart_DepositVoucher_RedemptionFailed_message,
                    failedDepositReturnVouchers.size,
                    message
                )
            )
            .setPositiveButton(R.string.Snabble_ShoppingCart_DepositVoucher_RedemptionFailed_button) { _, _ ->
                failedDepositReturnVouchers
                    .map { it.refersTo }
                    .forEach(cart::removeItem)
            }
            .show()
    }

    private fun getDisplayName(depositReturnVoucher: DepositReturnVoucher): String? =
        cart.getByItemId(depositReturnVoucher.refersTo)
            ?.displayName
            ?.let { price -> "${context.getString(R.string.Snabble_ShoppingCart_DepositReturn_title)}: $price" }

    private fun getMaxSubjectLength(): Int? = Snabble.checkedInProject.value
        ?.id
        ?.let { id ->
            Snabble.customProperties
                .getOrDefault(
                    ExternalBillingSubjectLength to ProjectId(id),
                    defaultValue = null
                )
                ?.toString()
                ?.toInt()
        }
}

fun interface CheckoutPreconditionHandler {

    fun isCheckoutAllowed(): Boolean
}
