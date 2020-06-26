/*
 * This file is part of GNU Taler
 * (C) 2020 Taler Systems S.A.
 *
 * GNU Taler is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3, or (at your option) any later version.
 *
 * GNU Taler is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GNU Taler; see the file COPYING.  If not, see <http://www.gnu.org/licenses/>
 */

package org.gnu.anastasis.ui.authentication

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager.beginDelayedTransition
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialContainerTransform.FADE_MODE_CROSS
import kotlinx.android.synthetic.main.fragment_video.*
import org.gnu.anastasis.ui.MainViewModel
import org.gnu.anastasis.ui.R
import java.io.FileDescriptor

private const val REQUEST_IMAGE_CAPTURE = 1
private const val REQUEST_IMAGE_OPEN = 2

class VideoFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            fadeMode = FADE_MODE_CROSS
        }
        return inflater.inflate(R.layout.fragment_video, container, false).apply {
            transitionName = "video_card"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        takePhotoButton.setOnClickListener {
            val pm = requireContext().packageManager
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(pm)?.also {
                    startActivityForResult(takePictureIntent,
                        REQUEST_IMAGE_CAPTURE
                    )
                }
            }
        }
        choosePhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent,
                REQUEST_IMAGE_OPEN
            )
        }

        saveVideoButton.setOnClickListener {
            viewModel.videoChecked.value = true
            findNavController().popBackStack()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data!!.extras!!.get("data") as Bitmap
            showImage(imageBitmap)
        } else if (requestCode == REQUEST_IMAGE_OPEN && resultCode == RESULT_OK) {
            data?.data?.also { uri ->
                val imageBitmap = getBitmapFromUri(uri)
                showImage(imageBitmap)
            }
        }
    }

    private fun showImage(bitmap: Bitmap) {
        photoView.setImageBitmap(bitmap)
        beginDelayedTransition(view as ViewGroup)
        photoView.visibility = VISIBLE
        takePhotoButton.visibility = GONE
        choosePhotoButton.visibility = GONE
        saveVideoButton.isEnabled = true
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val contentResolver = requireContext().contentResolver
        val parcelFileDescriptor: ParcelFileDescriptor =
            contentResolver.openFileDescriptor(uri, "r")!!
        val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    }

}
