package com.meetme.meetmeclient.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.meetme.meetmeclient.MapsActivity
import com.meetme.meetmeclient.R
import com.meetme.meetmeclient.profile.UserService.Companion.service
import kotlinx.android.synthetic.main.activity_profile.*
import org.json.JSONObject
import java.io.FileOutputStream


class ProfileActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CAMERA = 100
        const val USER_ID = "user_id"
        const val USERNAME = "username"
        const val USER = "user.txt"
        const val DESCRIPTION = "description"
        const val GENDER = "gender"
        const val SHARED = "shared"
    }

    private var imageView: ImageView? = null

    private fun saveUser(user: User) {
        val json = JSONObject()
        json.put(USERNAME, user.userName)
        json.put(DESCRIPTION, user.userDescription)
        json.put(GENDER, user.gender)

        val userMono = service.save(user)

        userMono.doOnNext {
            setEditMode(false)
            val file: String = USER
            val fileOutputStream: FileOutputStream
            try {
                fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
                fileOutputStream.write(it.userId?.toByteArray()!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            getSharedPreferences(SHARED, Context.MODE_PRIVATE).edit()
                .putString(USER_ID, it.userId).apply()
        }.doOnError {
            Log.e("error", "Received an exception ${it.stackTrace}")
            setEditMode(false)
            Toast.makeText(
                this@ProfileActivity, getString(R.string.server_error),
                Toast.LENGTH_LONG
            ).show()
        }.subscribe()
    }

    private fun readUserId(): String {
        return getSharedPreferences(SHARED, Context.MODE_PRIVATE).getString(USER_ID, "")!!
    }

    private fun getUser(userId: String) {
        val userMono = service.getUser(userId)
        userMono.doOnNext {
            Log.i("t", "$it")
            usernameEditText.setText(it.userName)
            descriptionEditText.setText(it.userDescription)
            genderEditText.setText(it.gender)
        }.doOnError {
            Log.e("error", "Received an exception $it")
            Toast.makeText(
                this@ProfileActivity, getString(R.string.server_error),
                Toast.LENGTH_LONG
            ).show()
        }.subscribe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        imageView = findViewById(R.id.profile)
        imageView?.setOnClickListener { selectImage() }

        val userId = readUserId()
        getUser(userId)
    }

    private fun selectImage() {
        val chooseOption = getString(R.string.choose_photo)
        val cancelOption = getString(R.string.cancel)
        val options =
            arrayOf<CharSequence>(chooseOption, cancelOption)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo!")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == chooseOption -> {
                    val intent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    startActivityForResult(intent, REQUEST_CAMERA)
                }
                options[item] == cancelOption -> dialog.dismiss()
            }
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            val image =
                BitmapFactory.decodeStream(
                    imageUri?.let { contentResolver.openInputStream(it) },
                    null,
                    null
                )
            imageView?.setImageBitmap(image)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> startActivity(Intent(this, MapsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    fun editHandler(v: View) {
        setEditMode(true)
    }

    fun cancelHandler(v: View) {
        setEditMode(false)
    }

    fun saveHandler(view: View) {
        setEditMode(false)

        val user = User(
            null,
            usernameEditText.text.toString(),
            descriptionEditText.text.toString(),
            genderEditText.text.toString()
        )
        if (isValid(user, view)) {
            saveUser(user)
        }
    }

    private fun isValid(user: User, view: View): Boolean {
        if (user.userName.isEmpty()) {
            Snackbar.make(view, getString(R.string.username_validation), Snackbar.LENGTH_SHORT)
                .show()
            return false
        }
        if (user.userDescription?.isEmpty()!!) {
            Snackbar.make(view, getString(R.string.description_validation), Snackbar.LENGTH_SHORT)
                .show()
            return false
        }
        if (user.gender?.isEmpty()!!) {
            Snackbar.make(view, getString(R.string.gender_validation), Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun setEditMode(editMode: Boolean) {
        setFieldEditable(usernameEditText, editMode)
        setFieldEditable(descriptionEditText, editMode)
        setFieldEditable(genderEditText, editMode)
        setButtonVisible(saveButton, editMode)
        setButtonVisible(cancelButton, editMode)

    }

    private fun setFieldEditable(field: EditText, editable: Boolean) {
        field.isFocusable = editable
        field.isClickable = editable
        field.isFocusableInTouchMode = editable
    }

    private fun setButtonVisible(button: Button, visible: Boolean) {
        button.isVisible = visible
    }

}
