package io.snabble.sdk.ui.cart

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.snabble.sdk.*
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.checkout.CheckoutHelper
import io.snabble.sdk.ui.databinding.SnabbleViewCheckoutBarBinding
import io.snabble.sdk.ui.payment.PaymentInputViewHelper
import io.snabble.sdk.ui.payment.SEPALegalInfoHelper
import io.snabble.sdk.ui.payment.SelectPaymentMethodFragment
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.*


class CheckoutBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), Checkout.OnCheckoutStateChangedListener {
    private lateinit var progressDialog: DelayedProgressDialog
    
    private val binding: SnabbleViewCheckoutBarBinding
    
    private val paymentSelectionHelper by lazy { PaymentSelectionHelper.getInstance() }
    private val project by lazy { SnabbleUI.getProject() }
    private val cart: ShoppingCart by lazy { project.shoppingCart }
    private val cartChangeListener = object : ShoppingCart.SimpleShoppingCartListener() {
        override fun onChanged(list: ShoppingCart?) = update()
    }

    val priceHeight: Int
        get() = binding.priceSum.height + binding.sumContainer.marginTop * 2

    init {
        LayoutInflater.from(context).inflate(R.layout.snabble_view_checkout_bar, this, true)
        binding = SnabbleViewCheckoutBarBinding.bind(this)
        
        orientation = VERTICAL

        if (!isInEditMode) {
            initBusinessLogic()
        }
    }

    private fun initBusinessLogic() {
        paymentSelectionHelper.selectedEntry.observe(UIUtils.getHostActivity(context) as FragmentActivity, {
            update()
        })

        binding.paymentSelectorButton.setOnClickListener {
            paymentSelectionHelper.showDialog(UIUtils.getHostFragmentActivity(context))
        }

        binding.paymentSelectorButtonBig.setOnClickListener {
            paymentSelectionHelper.showDialog(UIUtils.getHostFragmentActivity(context))
        }

        binding.pay.setOneShotClickListener {
            cart.taxation = ShoppingCart.Taxation.UNDECIDED
            payClick()
        }

        binding.googlePayButtonLayout.googlePayButton.setOneShotClickListener {
            val packageName = "com.google.android.apps.walletnfcrel"
            val pm = context.packageManager
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                payClick()
            } catch (e: PackageManager.NameNotFoundException) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")))
                } catch (e: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
            }
        }

        cart.addListener(cartChangeListener)
        update()

        progressDialog = DelayedProgressDialog(context)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setMessage(context.getString(R.string.Snabble_pleaseWait))
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(true)
        progressDialog.setOnKeyListener(DialogInterface.OnKeyListener { dialogInterface: DialogInterface, _, keyEvent: KeyEvent ->
            if (keyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                project.checkout.abort()
                dialogInterface.dismiss()
                return@OnKeyListener true
            }
            false
        })

        context.requireFragmentActivity().lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                if (isAttachedToWindow) {
                    registerListeners()
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop() {
                unregisterListeners()
            }
        })
    }

    private fun payClick() {
        if (cart.isRestorable) {
            cart.restore()
            update()
        } else {
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
            binding.paymentSelector.visibility = GONE
        } else {
            val pcs = Snabble.getInstance().paymentCredentialsStore
            val hasNoPaymentMethods = pcs.usablePaymentCredentialsCount == 0
            val isHidden = project.paymentMethodDescriptors.size == 1 && hasNoPaymentMethods
            binding.paymentSelector.visibility = if (isHidden) GONE else VISIBLE
            binding.paymentIcon.setImageResource(entry.iconResId)
        }
    }

    private fun updatePayAndText() {
        cart.let { cart ->
            val quantity = cart.totalQuantity
            val price = cart.totalPrice
            val articlesText = resources.getQuantityText(R.plurals.Snabble_Shoppingcart_numberOfItems, quantity)
            binding.articleCount.text = String.format(articlesText.toString(), quantity)
            binding.priceSum.text = project.priceFormatter.format(price)

            val onlinePaymentAvailable = cart.availablePaymentMethods != null && cart.availablePaymentMethods.isNotEmpty()
            binding.pay.isEnabled = price > 0 && (onlinePaymentAvailable || paymentSelectionHelper.selectedEntry.value != null)

            val showBigSelector = paymentSelectionHelper.shouldShowBigSelector()
            val entry = paymentSelectionHelper.selectedEntry.value
            if (entry?.paymentMethod == PaymentMethod.GOOGLE_PAY && price > 0) {
                binding.pay.isVisible = false
                binding.googlePayButtonLayout.googlePayButton.isVisible = true
            } else {
                binding.pay.isVisible = true
                binding.googlePayButtonLayout.googlePayButton.isVisible = false
            }

            binding.paymentSelectorButtonBig.isVisible = showBigSelector
            binding.paymentActive.isVisible = !showBigSelector
            binding.pay.isVisible = !showBigSelector

            if (cart.isRestorable) {
                binding.pay.isEnabled = true
                binding.pay.setText(R.string.Snabble_Shoppingcart_emptyState_restoreButtonTitle)
            } else {
                binding.pay.setText(I18nUtils.getIdentifierForProject(resources, project, R.string.Snabble_Shoppingcart_buyProducts_now))
            }
        }
    }

    private fun pay() {
        if (cart.hasReachedMaxCheckoutLimit()) {
            val message = resources.getString(R.string.Snabble_limitsAlert_checkoutNotAvailable,
                    project.priceFormatter.format(project.maxCheckoutLimit))
            SnackbarUtils.make(this, message, UIUtils.SNACKBAR_LENGTH_VERY_LONG).show()
        } else {
            val entry = paymentSelectionHelper.selectedEntry.value
            if (entry != null) {
                if (entry.paymentMethod.isRequiringCredentials && entry.paymentCredentials == null) {
                    PaymentInputViewHelper.openPaymentInputView(context, entry.paymentMethod, project.id)
                } else {
                    Telemetry.event(Telemetry.Event.ClickCheckout)
                    SEPALegalInfoHelper.showSEPALegalInfoIfNeeded(context,
                            entry.paymentMethod,
                            object : OneShotClickListener() {
                                override fun click() {
                                    if (entry.paymentMethod.isOfflineMethod) {
                                        project.checkout.checkout(3000)
                                    } else {
                                        project.checkout.checkout()
                                    }
                                }
                            })
                }
            } else {
                var hasPaymentMethodThatRequiresCredentials = false
                val paymentMethodsDescriptors = project.paymentMethodDescriptors
                if (paymentMethodsDescriptors != null && paymentMethodsDescriptors.isNotEmpty()) {
                    for (descriptor in paymentMethodsDescriptors) {
                        if (descriptor.paymentMethod.isRequiringCredentials) {
                            hasPaymentMethodThatRequiresCredentials = true
                            break
                        }
                    }
                }
                if (hasPaymentMethodThatRequiresCredentials) {
                    val activity = UIUtils.getHostActivity(context)
                    if (activity is FragmentActivity) {
                        val dialogFragment = SelectPaymentMethodFragment()
                        val bundle = Bundle()
                        bundle.putString(SelectPaymentMethodFragment.ARG_PROJECT_ID, SnabbleUI.getProject().id)
                        dialogFragment.arguments = bundle
                        dialogFragment.show(activity.supportFragmentManager, null)
                    }
                } else {
                    Toast.makeText(context, R.string.Snabble_Payment_errorStarting, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun registerListeners() {
        project.checkout.addOnCheckoutStateChangedListener(this)
    }

    private fun unregisterListeners() {
        project.checkout.removeOnCheckoutStateChangedListener(this)
        progressDialog.dismiss()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            registerListeners()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterListeners()
    }

    override fun onStateChanged(state: Checkout.State) {
        if (state == Checkout.State.HANDSHAKING) {
            progressDialog.showAfterDelay(300)
        } else if (state == Checkout.State.REQUEST_PAYMENT_METHOD) {
            val entry = paymentSelectionHelper.selectedEntry.value
            if (entry == null) {
                progressDialog.dismiss()
                Toast.makeText(context, R.string.Snabble_Payment_errorStarting, Toast.LENGTH_LONG).show()
                return
            }
            if (entry.paymentCredentials != null) {
                progressDialog.dismiss()
                if (entry.paymentMethod == PaymentMethod.TEGUT_EMPLOYEE_CARD) {
                    project.checkout.pay(entry.paymentMethod, entry.paymentCredentials)
                } else {
                    Keyguard.unlock(UIUtils.getHostFragmentActivity(context), object : Keyguard.Callback {
                        override fun success() {
                            progressDialog.showAfterDelay(300)
                            project.checkout.pay(entry.paymentMethod, entry.paymentCredentials)
                        }

                        override fun error() {
                            progressDialog.dismiss()
                        }
                    })
                }
            } else {
                progressDialog.showAfterDelay(300)
                project.checkout.pay(entry.paymentMethod, null)
            }
        } else if (state == Checkout.State.WAIT_FOR_APPROVAL) {
            CheckoutHelper.displayPaymentView(UIUtils.getHostFragmentActivity(context), project.checkout)
            progressDialog.dismiss()
        } else if (state == Checkout.State.PAYMENT_PROCESSING) {
            progressDialog.showAfterDelay(300)
        } else if (state == Checkout.State.PAYMENT_APPROVED) {
            Telemetry.event(Telemetry.Event.CheckoutSuccessful)
            SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_SUCCESS)
        } else if (state == Checkout.State.DENIED_BY_PAYMENT_PROVIDER) {
            Telemetry.event(Telemetry.Event.CheckoutDeniedByPaymentProvider)
            SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_FAILURE)
        } else if (state == Checkout.State.DENIED_BY_SUPERVISOR) {
            Telemetry.event(Telemetry.Event.CheckoutDeniedBySupervisor)
            SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_FAILURE)
        } else if (state == Checkout.State.INVALID_PRODUCTS) {
            val invalidProducts = project.checkout.invalidProducts
            if (invalidProducts != null && invalidProducts.size > 0) {
                val res = resources
                val sb = StringBuilder()
                if (invalidProducts.size == 1) {
                    sb.append(I18nUtils.getIdentifier(res, R.string.Snabble_saleStop_errorMsg_one))
                } else {
                    sb.append(I18nUtils.getIdentifier(res, R.string.Snabble_saleStop_errorMsg))
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
                        .setTitle(I18nUtils.getIdentifier(resources, R.string.Snabble_saleStop_errorMsg_title))
                        .setMessage(sb.toString())
                        .setPositiveButton(R.string.Snabble_OK, null)
                        .show()
            } else {
                SnackbarUtils.make(this, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG).show()
            }
            progressDialog.dismiss()
        } else if (state == Checkout.State.CONNECTION_ERROR
            || state == Checkout.State.NO_SHOP
            || state == Checkout.State.PAYMENT_PROCESSING_ERROR) {
            SnackbarUtils.make(this, R.string.Snabble_Payment_errorStarting, UIUtils.SNACKBAR_LENGTH_VERY_LONG).show()
            progressDialog.dismiss()
        } else if (state == Checkout.State.PAYMENT_ABORTED) {
            progressDialog.dismiss()
        } else if (state == Checkout.State.REQUEST_VERIFY_AGE) {
            SnabbleUI.executeAction(SnabbleUI.Action.SHOW_AGE_VERIFICATION)
            progressDialog.dismiss()
        } else if (state == Checkout.State.REQUEST_TAXATION) {
            progressDialog.dismiss()
            val dialog = AlertDialog.Builder(context)
                .setTitle(I18nUtils.getIdentifier(context.resources, R.string.Snabble_Taxation_consumeWhere))
                .setAdapter(
                    ArrayAdapter(context, R.layout.snabble_item_taxation, listOf(
                        context.getString(R.string.Snabble_Taxation_consume_inhouse),
                        context.getString(R.string.Snabble_Taxation_consume_takeaway)
                    ))
                ) { dialog, which ->
                    if (which == 0) {
                        cart.taxation = ShoppingCart.Taxation.IN_HOUSE
                    } else {
                        cart.taxation = ShoppingCart.Taxation.TAKEAWAY
                    }
                    dialog.dismiss()
                    project.checkout.checkout()
                }
                .create()
                .show()
        } else if (state == Checkout.State.NO_PAYMENT_METHOD_AVAILABLE) {
            AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(I18nUtils.getIdentifier(resources, R.string.Snabble_saleStop_errorMsg_title))
                    .setMessage(I18nUtils.getIdentifier(resources, R.string.Snabble_Payment_noMethodAvailable))
                    .setPositiveButton(R.string.Snabble_OK, null)
                    .show()
            progressDialog.dismiss()
        }
    }
}