package com.jessicathornsby.rxjavasignupscreen

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import com.jakewharton.rxbinding2.widget.RxTextView




class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//Respond to text change events in enterEmail//

        RxTextView.afterTextChangeEvents(enterEmail)

//Skip enterEmail’s initial, empty state//

                .skipInitialValue()
                .map {
                    emailError.error = null
                    it.view().text.toString()
                }

//Ignore all emissions that occur within a 400 milliseconds timespan//

                .debounce(400,

//Make sure we’re in Android’s main UI thread//

                        TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())

//Apply the validateEmailAddress transformation function//

                .compose(validateEmailAddress)
                .compose(retryWhenError {
                    emailError.error = it.message
                })
                .subscribe()

//Rinse and repeat for the enterPassword EditText//

        RxTextView.afterTextChangeEvents(enterPassword)
                .skipInitialValue()
                .map {
                    passwordError.error = null
                    it.view().text.toString()
                }
                .debounce(400, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .compose(validatePassword)
                .compose(retryWhenError {
                    passwordError.error = it.message
                })
                .subscribe()
    }

//If the app encounters an error, then try again//

    private inline fun retryWhenError(crossinline onError: (ex: Throwable) -> Unit): ObservableTransformer<String, String> = ObservableTransformer { observable ->
        observable.retryWhen { errors ->

            ///Use the flatmap() operator to flatten all emissions into a single Observable//

            errors.flatMap {
                onError(it)
                Observable.just("")
            }

        }
    }

//Define our ObservableTransformer and specify that the input and output must be a string//

    private val validatePassword = ObservableTransformer<String, String> { observable ->
        observable.flatMap {
            Observable.just(it).map { it.trim() }

//Check that the password is at least 7 characters long//

                    .filter { it.length > 7 }

//If the password is less than 7 characters, then throw an error//

                    .singleOrError()

//If an error occurs, then display the following message//

                    .onErrorResumeNext {
                        if (it is NoSuchElementException) {
                            Single.error(Exception("Your password must be 7 characters or more"))

                        } else {
                            Single.error(it)
                        }
                    }
                    .toObservable()


        }
    }

//Define an ObservableTransformer, where we’ll perform the email validation//

    private val validateEmailAddress = ObservableTransformer<String, String> { observable ->
        observable.flatMap {
            Observable.just(it).map { it.trim() }

//Check whether the user input matches Android’s email pattern//

                    .filter {
                        Patterns.EMAIL_ADDRESS.matcher(it).matches()

                    }

//If the user’s input doesn’t match the email pattern, then throw an error//

                    .singleOrError()
                    .onErrorResumeNext {
                        if (it is NoSuchElementException) {
                            Single.error(Exception("Please enter a valid email address"))
                        } else {
                            Single.error(it)

                        }
                    }
                    .toObservable()
        }
    }



}
