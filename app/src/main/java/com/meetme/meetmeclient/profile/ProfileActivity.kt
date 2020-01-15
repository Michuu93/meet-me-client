package com.meetme.meetmeclient.profile

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.meetme.meetmeclient.MapsActivity
import com.meetme.meetmeclient.R


class ProfileActivity : AppCompatActivity() {

    private var editMode: Boolean = false;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)


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

    fun saveHandler(v: View) {
        setEditMode(false)

        val (usernameField, descriptionField, genderField) = getUserForm()
        val user = User(
            usernameField.text.toString(),
            descriptionField.text.toString(),
            genderField.text.toString()
        )
        //TODO save
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
    }

    private fun setButtonVisible(button: Button, visible: Boolean) {
        button.isVisible = visible
    }


}
