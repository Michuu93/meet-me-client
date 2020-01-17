package com.meetme.meetmeclient.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
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
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*


class ProfileActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CAMERA = 100
        const val USERNAME = "username"
        const val USER = "user.txt"
        const val DESCRIPTION = "description"
        const val GENDER = "gender"

    }


    private var imageView: ImageView? = null

    private fun saveUser(user: User) {

        val json = JSONObject()
        json.put(USERNAME, user.userName)
        json.put(DESCRIPTION, user.userDescription)
        json.put(GENDER, user.gender)

        val call = service.save(user)

        call.enqueue(object : Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.e("error", "Received an exception $t")
                setEditMode(false)
                Toast.makeText(
                    this@ProfileActivity, getString(R.string.server_error),
                    Toast.LENGTH_LONG
                ).show()

            }

            override fun onResponse(call: Call<User>, response: Response<User>) {
                setEditMode(false)
                if (response.code() == 200) {
                    val userResponse = response.body()!!
                    saveUserId(userResponse.userId)
                }

            }

            private fun saveUserId(userId: String?) {
                val file: String = USER
                val fileOutputStream: FileOutputStream
                try {
                    fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
                    fileOutputStream.write(userId?.toByteArray())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        })
    }

    private fun readUserId(): String {
        var fileInputStream: FileInputStream? = null
        fileInputStream = openFileInput(USER)
        val inputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader = BufferedReader(inputStreamReader)
        val stringBuilder: StringBuilder = StringBuilder()
        var text: String? = null
        while ({ text = bufferedReader.readLine(); text }() != null) {
            stringBuilder.append(text)
        }
        return stringBuilder.toString()
    }

    private fun getUser(userId: String) {
        val call = service.getUser(userId)
        call.enqueue(object : Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.e("error", "Received an exception $t")
                Toast.makeText(
                    this@ProfileActivity, getString(R.string.server_error),
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.code() == 200) {
                    val userResponse = response.body()!!

                    Log.i("t", "$userResponse")
                    val (usernameField, descriptionField, genderField) = getUserForm()

                    usernameField.setText(userResponse.userName)
                    descriptionField.setText(userResponse.userDescription)
                    genderField.setText(userResponse.gender)

                } else {
                    Toast.makeText(
                        this@ProfileActivity, getString(R.string.update_profile),
                        Toast.LENGTH_LONG
                    ).show()
                    setEditMode(true)
                }
            }

        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        imageView = findViewById(R.id.profile)
        imageView?.setOnClickListener { selectImage() }

        prepareUserFile()
        val userId = readUserId()
        getUser(userId)

    }

    private fun prepareUserFile() {
        val file = File(getBaseContext().getFilesDir(), USER)
        if (!file.exists()) {
            file.createNewFile()
        }
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
            R.id.home -> startActivity(Intent(this, MapsActivity::class.java));
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

        val (usernameField, descriptionField, genderField) = getUserForm()
        val user = User(
            null,
            usernameField.text.toString(),
            descriptionField.text.toString(),
            genderField.text.toString()
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
        val (usernameField, descriptionField, genderField) = getUserForm()
        val saveButton = findViewById<Button>(R.id.save)
        val cancelButton = findViewById<Button>(R.id.cancel)

        setFieldEditable(usernameField, editMode)
        setFieldEditable(descriptionField, editMode)
        setFieldEditable(genderField, editMode)
        setButtonVisible(saveButton, editMode)
        setButtonVisible(cancelButton, editMode)

    }

    private fun getUserForm(): Triple<EditText, EditText, EditText> {
        val usernameField = findViewById<EditText>(R.id.username)
        val descriptionField = findViewById<EditText>(R.id.description)
        val genderField = findViewById<EditText>(R.id.gender)
        return Triple(usernameField, descriptionField, genderField)
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
