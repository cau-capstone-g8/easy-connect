package kr.ac.cau.easyconnect

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ClipData
import android.content.DialogInterface
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class WriteActivity : AppCompatActivity() {

    var firebaseAuth: FirebaseAuth? = null
    var storage : FirebaseStorage? = null

    val REQUEST_IMAGE_CAPTURE = 1
    val REQUEST_GALLERY_TAKE = 2
    val GET_GALLERY_IMAGE = 200

    var imgFileName : String? = null
    var imgNameList: Array<String?> = arrayOfNulls(3)
    var uriPhoto : Uri? = null
    var clipData: ClipData? = null

    var uriList: Array<Uri?> = arrayOfNulls(3)

    lateinit var currentPhotoPath : String
    lateinit var imageContainer : LinearLayout
    lateinit var imageView : ImageView
    lateinit var imageView2 : ImageView
    lateinit var imageView3 : ImageView
    lateinit var content: MultiAutoCompleteTextView
    val timestamp: String = SimpleDateFormat("yyyyMMdd_HHmmsss").format(Date())

    private var speechRecognizer: SpeechRecognizer? = null
    private var REQUEST_CODE = 1

    lateinit var userData : UserDTO
    lateinit var age : String
    lateinit var gender : String

    lateinit var checkAge : String
    lateinit var checkGender : String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)
        if (Build.VERSION.SDK_INT >= 23)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO), REQUEST_CODE)

        // Firebase - Auth, Firestore??? ???????????? ????????????
        firebaseAuth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // xml id ??????
//        val title: EditText = findViewById(R.id.title)
        content = findViewById(R.id.content)
        imageView = findViewById(R.id.imageView)
        imageView2 = findViewById(R.id.imageView2)
        imageView3 = findViewById(R.id.imageView3)
        imageContainer = findViewById(R.id.imageContainer)

        //????????? ?????? ????????????
        db.collection("user_information").whereEqualTo("email", firebaseAuth!!.currentUser.email).get().addOnCompleteListener {
            for (dc in it.result!!.documents) {
                var data = dc.toObject(UserDTO::class.java)
                if (data != null) {
                    userData = data
                }
                break
            }

            age = userData!!.age.toString()
            gender = userData!!.gender.toString()
        }

        findViewById<ImageButton>(R.id.back).setOnClickListener {

            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("????????? ????????? ???????????? ????????????. \n????????????????????????? ")
            // ????????? ?????? ?????? ??? ?????????
            var listener = DialogInterface.OnClickListener { dialog, i ->
                val intentMain = Intent(this, MainActivity::class.java)
                startActivity(intentMain)
                finish()
            }
            dialog.setPositiveButton("??????", listener)
            dialog.setNegativeButton("??????", null)
            dialog.show()
        }
        // ???????????? ????????? ??? ??????
        findViewById<Button>(R.id.cancel).setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("????????? ????????? ???????????? ????????????. \n????????????????????????? ")
            // ????????? ?????? ?????? ??? ?????????
            var listener = DialogInterface.OnClickListener { dialog, i ->
                val intentMain = Intent(this, MainActivity::class.java)
                startActivity(intentMain)
                finish()
            }
            dialog.setPositiveButton("??????", listener)
            dialog.setNegativeButton("??????", null)
            dialog.show()
        }

        // ????????????
        findViewById<Button>(R.id.photo).setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("?????? ????????? ?????? ??????")
            // ?????? ?????? ??????
            var cameraListener = DialogInterface.OnClickListener { dialog, i ->
                takePicture()
            }
            // ?????? ?????? ??????
            var albumListener = DialogInterface.OnClickListener { dialog, i ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_GALLERY_TAKE)
            }
            dialog.setPositiveButton("??????", cameraListener)
            dialog.setNeutralButton("??????", null)
            dialog.setNegativeButton("??????", albumListener)
            dialog.show()
        }

        // ???????????? ????????? ??? ??????

        findViewById<Button>(R.id.post).setOnClickListener {
    //        var inputTitle = title.text.trim().toString()
            var inputContent = content.text.trim().toString()
            var name = firebaseAuth!!.currentUser.email.toString()
            var registered : String = LocalDateTime.now().toString()
            var modified : String = LocalDateTime.now().toString()
            var imgOfDetail : String? = imgNameList[0]
            var imgOfDetail2 : String? = imgNameList[1]
            var imgOfDetail3 : String? = imgNameList[2]

            var hashtagList : MutableList<String> = mutableListOf() // ???????????? ?????? ?????? ?????? ??????
            val splitArray = content.text.split(" ")    // ????????? ???????????? ????????? ????????? ????????? ??????
            for (a in splitArray) {     // ??????????????? ????????? ??????
                if ('#' in a) {
                    hashtagList.add(a.substring(a.indexOf("#")))    // #?????? ????????? ?????? ????????? ?????? ?????????
               }
            }
            // ???????????? ??????
            // TODO: ?????? ????????? ?????? ??? ???????????? ???????????? ????????? ???????????????
            var htDTO : HashDTO? = null

            // ?????? total case
            for (ht in hashtagList) {
                db.collection("hashtag/total/name").whereEqualTo("name", ht).get().addOnCompleteListener{
                    htDTO = null
                    if(it.isSuccessful){
                        for (dc in it.result!!.documents) {
                            htDTO = dc.toObject(HashDTO::class.java)
                            break
                        }

                        var map = mutableMapOf<String, Any?>()
                        if(htDTO != null){
                            map["count"] = htDTO!!.count!!.toInt() + 1

                            db.collection("hashtag/total/name").document(
                                ht
                            ).update(map)
                        }
                        else{
                            var new_count = 1
                            val new_htDTO = HashDTO(ht, new_count)
                            db.collection("hashtag/total/name").document(ht).set(new_htDTO)
                        }
                    }
                }
            }

            var myDTO = UserDTO()
            db!!.collection("user_information").whereEqualTo("email", firebaseAuth!!.currentUser.email).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    for (dc in it.result!!.documents) {
                        myDTO = dc.toObject(UserDTO::class.java)!!
                        break
                    }

                    val age = myDTO.age!!.toInt()
                    var current_age : String? = null
                    if(age < 20){
                        // 20??? ??????
                        current_age = "upto20"
                    }else if(age < 30){
                        // 20???
                        current_age = "age20s"
                    }else if(age < 40){
                        // 30???
                        current_age = "age30s"
                    }else if(age < 50){
                        // 40???
                        current_age = "age40s"
                    }else{
                        // 50??? ??????
                        current_age = "over50"
                    }

                    // ?????? case
                    for (ht in hashtagList) {
                        db.collection("hashtag/" + current_age + "/name").whereEqualTo("name", ht).get().addOnCompleteListener{ query ->
                            htDTO = null

                            if(query.isSuccessful){
                                for (dc in query.result!!.documents) {
                                    htDTO = dc.toObject(HashDTO::class.java)
                                    break
                                }

                                var map = mutableMapOf<String, Any?>()
                                if(htDTO != null){
                                    map["count"] = htDTO!!.count!!.toInt() + 1

                                    db.collection("hashtag/" + current_age + "/name").document(
                                        ht
                                    ).update(map)
                                }
                                else{
                                    var new_count = 1
                                    val new_htDTO = HashDTO(ht, new_count)
                                    db.collection("hashtag/" + current_age + "/name").document(ht).set(new_htDTO)
                                }
                            }
                        }
                    }
                }
            }

            var myDTO2 = UserDTO()
            db!!.collection("user_information").whereEqualTo("email", firebaseAuth!!.currentUser.email).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    for (dc in it.result!!.documents) {
                        myDTO2 = dc.toObject(UserDTO::class.java)!!
                        break
                    }
                    val current_gender = myDTO2.gender

                    // ?????? case
                    for (ht in hashtagList) {
                        db.collection("hashtag/" + current_gender + "/name").whereEqualTo("name", ht).get().addOnCompleteListener{ query ->
                            htDTO = null
                            if(query.isSuccessful){
                                for (dc in query.result!!.documents) {
                                    htDTO = dc.toObject(HashDTO::class.java)
                                    break
                                }

                                var map = mutableMapOf<String, Any?>()
                                if(htDTO != null){
                                    map["count"] = htDTO!!.count!!.toInt() + 1

                                    db.collection("hashtag/" + current_gender + "/name").document(ht).update(map)
                                }
                                else{
                                    var new_count = 1
                                    val new_htDTO = HashDTO(ht, new_count)
                                    db.collection("hashtag/" + current_gender + "/name").document(ht).set(new_htDTO)
                                }
                            }
                        }
                    }
                }
            }
            val postDTO : PostDTO = PostDTO(null, inputContent, name, registered, modified,
                imgOfDetail, imgOfDetail2, imgOfDetail3)

            if (inputContent.isNullOrEmpty()) {
                var builder = AlertDialog.Builder(this)
                builder.setTitle("????????? ??????????????????.")
                builder.setPositiveButton("??????", null)
                builder.show()
            } else {
                if (!imgOfDetail.isNullOrEmpty()){
                    imageUpload()
                    db.collection("post").document(registered).set(postDTO).addOnCompleteListener(this) {
                        //?????? ??????????????? ?????? ?????? ???
                        if (it.isSuccessful) {
                            //Toast.makeText(this, "success", Toast.LENGTH_SHORT).show()
                            //?????? ???????????? ???????????? ?????? ??? ??? ???????????? ??????????????? ??????. ????????? ?????? ??????
                            //val intent = Intent(this, DetailActivity::class.java)
                            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)   Detail??? ????????? ?????? ???????????? ?????? ?????? ??????
                            //startActivity(intent)
                            val loadingAnimDialog = CustomLodingDialog(this)
                            loadingAnimDialog.show()
                            Handler().postDelayed({
                                loadingAnimDialog.dismiss()
                                val intentMain = Intent(this, MainActivity::class.java)
                                startActivity(intentMain)
                                finish()
                            }, 15000)


                        } else {
                            Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }else{
                    db.collection("post").document(registered).set(postDTO).addOnCompleteListener(this){
                        if (it.isSuccessful) {
                            val intentMain = Intent(this, MainActivity::class.java)
                            startActivity(intentMain)
                            finish()
                        }else {
                            Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        // ??? ?????? ?????? ?????? ?????? ??????
        imageView.setOnLongClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("????????? ?????? ??????????????????? ")

            var listener = DialogInterface.OnClickListener { dialog, i ->
                storage = FirebaseStorage.getInstance()
                imageView.visibility = View.GONE
                imgNameList[0] = null
                if (imgNameList[0] == null && imgNameList[1] == null && imgNameList[2] == null) imageContainer.visibility = View.GONE
            }
            dialog.setPositiveButton("??????", listener)
            dialog.setNegativeButton("??????", null)
            dialog.show()
            return@setOnLongClickListener true
        }

        // ??? ?????? ?????? ?????? ?????? ??????
        imageView2.setOnLongClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("????????? ?????? ??????????????????? ")

            var listener = DialogInterface.OnClickListener { dialog, i ->
                storage = FirebaseStorage.getInstance()
                imageView2.visibility = View.GONE
                imgNameList[1] = null
                if (imgNameList[0] == null && imgNameList[1] == null && imgNameList[2] == null) imageContainer.visibility = View.GONE
            }
            dialog.setPositiveButton("??????", listener)
            dialog.setNegativeButton("??????", null)
            dialog.show()
            return@setOnLongClickListener true
        }

        // ??? ?????? ?????? ?????? ?????? ??????
        imageView3.setOnLongClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("????????? ?????? ??????????????????? ")

            var listener = DialogInterface.OnClickListener { dialog, i ->
                storage = FirebaseStorage.getInstance()
                imageView3.visibility = View.GONE
                imgNameList[2] = null
                if (imgNameList[0] == null && imgNameList[1] == null && imgNameList[2] == null) imageContainer.visibility = View.GONE
            }
            dialog.setPositiveButton("??????", listener)
            dialog.setNegativeButton("??????", null)
            dialog.show()
            return@setOnLongClickListener true
        }

        // ????????????
        findViewById<Button>(R.id.record).setOnClickListener {
            startSTTUseActivityResult()
        }
/*
        //????????? ????????? ????????????
//        var myDTO = UserDTO()
//        db!!.collection("user_information").whereEqualTo("email", firebaseAuth!!.currentUser.email).get().addOnCompleteListener {
//            if (it.isSuccessful) {
//                for (dc in it.result!!.documents) {
//                    myDTO = dc.toObject(UserDTO::class.java)!!
//                    break
//                }
//
//                val age = myDTO.age!!.toInt()
//                var current_age: String? = null
//                if (age < 20) {
//                    // 20??? ??????
//                    current_age = "upto20"
//                } else if (age < 30) {
//                    // 20???
//                    current_age = "age20s"
//                } else if (age < 40) {
//                    // 30???
//                    current_age = "age30s"
//                } else if (age < 50) {
//                    // 40???
//                    current_age = "age40s"
//                } else {
//                    // 50??? ??????
//                    current_age = "over50"
//                }
//                db.collection("hashtag/" + current_age + "/name").get()
//                    .addOnCompleteListener { query ->
//                        var hashDTO: HashDTO? = null
//                        var nameObject: MutableList<String> = mutableListOf()
//                        var countObject: MutableList<Int> = mutableListOf()
//                        var hashObject : MutableList<WriteActivity.Movie?> = mutableListOf()
//
//                        if (query.isSuccessful) {
//                            var i = 0
//                            for (dc in query.result!!.documents) {
//                                hashDTO = dc.toObject(HashDTO::class.java)
//                                hashObject.add(Movie(hashDTO!!.name.toString(), hashDTO!!.count!!.toInt()))
//
//                            }
//                            hashObject.sortByDescending { it!!.year }
//                            val adapter = AutoCompleteAdapter(this, R.layout.item_auto_complete_text_view, hashObject)
//                            content.threshold = 2  // ??? ???????????? ????????????
//                            content.setAdapter(adapter)
//                            content.setTokenizer(SpaceTokenizer())
//                        }
//                    }
//            }
//        }

        var myDTO = UserDTO()
        db!!.collection("user_information").whereEqualTo("email", firebaseAuth!!.currentUser.email).get().addOnCompleteListener {
            if (it.isSuccessful) {
                for (dc in it.result!!.documents) {
                    myDTO = dc.toObject(UserDTO::class.java)!!
                    break
                }

                val age = myDTO.age!!.toInt()
                var current_age: String? = null
                if (age < 20) {
                    // 20??? ??????
                    current_age = "upto20"
                } else if (age < 30) {
                    // 20???
                    current_age = "age20s"
                } else if (age < 40) {
                    // 30???
                    current_age = "age30s"
                } else if (age < 50) {
                    // 40???
                    current_age = "age40s"
                } else {
                    // 50??? ??????
                    current_age = "over50"
                }
                db.collection("hashtag/" + current_age + "/name").get()
                    .addOnCompleteListener { query ->
                        var hashDTO: HashDTO? = null
                        var nameObject: MutableList<String> = mutableListOf()
                        var countObject: MutableList<Int> = mutableListOf()
                        var hashObject : MutableList<WriteActivity.Movie?> = mutableListOf()

                        if (query.isSuccessful) {
                            var i = 0
                            for (dc in query.result!!.documents) {
                                hashDTO = dc.toObject(HashDTO::class.java)
                                hashObject.add(Movie(hashDTO!!.name.toString(), hashDTO!!.count!!.toInt()))

                            }
                            hashObject.sortByDescending { it!!.year }
                            val adapter = AutoCompleteAdapter(this, R.layout.item_auto_complete_text_view, hashObject)
                            content.threshold = 2  // ??? ???????????? ????????????
                            content.setAdapter(adapter)
                            content.setTokenizer(SpaceTokenizer())
                        }
                    }
            }
        }

 */
//
//        content.threshold = 1  // ??? ???????????? ????????????
//        content.setAdapter(adapter)    // ????????? ??????
//        content.setTokenizer(SpaceTokenizer()) // ???????????? ???????????? ??????

    }

    // STT ??????
    private fun startSTTUseActivityResult() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        startActivityForResult(speechRecognizerIntent, 100)
    }



    // ???????????? ??????
    override fun onBackPressed(){
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("????????? ????????? ???????????? ????????? ????????????.\n????????????????????????? ")
        // ????????? ?????? ?????? ??? ?????????
        var listener = DialogInterface.OnClickListener { dialog, i ->
            val intentMain = Intent(this, MainActivity::class.java)
            startActivity(intentMain)
            finish()
        }
        dialog.setPositiveButton("??????", listener)
        dialog.setNegativeButton("??????", null)
        dialog.show()
    }
    // ????????? ??????
    private fun takePicture() {
        storage = FirebaseStorage.getInstance()

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                // ?????? ?????? ???????????? ??????
                val photoFile: File? =
                        try {
                            createImageFile()
                        } catch (ex: IOException) {
                            Log.d("TAG", "???????????? ??????????????? ????????????")
                            null
                        }
                // onActivityForResult??? ??????
                photoFile?.also {
                    val photoUri: Uri = FileProvider.getUriForFile(
                            this, "kr.ac.cau.easyconnect.fileprovider", it
                    )

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }
    // ???????????? ????????? ???????????? ????????? ???????????????
    @Throws(IOException::class)
    private  fun createImageFile(): File {
        // Create an image file name
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imgFileName = "IMAGE_" + timestamp + "_.jpg"

        return File.createTempFile(
                "JPEG_${timestamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }

    }

    override fun onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer!!.stopListening()
        }

        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == 100) {
            val st: String = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!![0]
            content.setText(st)
        //    content.text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!![0]
        }
        when(requestCode) {
            // ????????? ??????
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    // ?????????????????? ?????? ????????? ?????????
                    imageView.visibility = View.VISIBLE
                    imageContainer.visibility = View.VISIBLE
                    val file = File(currentPhotoPath)
                    val decode = ImageDecoder.createSource(
                            this.contentResolver,
                            Uri.fromFile(file)
                    )
                    val bitmap = ImageDecoder.decodeBitmap(decode)
                    imageView.setImageBitmap(bitmap)
                    uriList[0] = Uri.fromFile(file)
                    imgNameList[0] = "IMAGE_" + timestamp + "_.jpg"
                }
                Toast.makeText(this, "????????? ?????? ???????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show()
                val labels = ImageAnnotator.batchAnnotateImages(applicationContext, uriList.toList())
                content.text.append(labels.joinToString(" #", " #"))
            }
            // ???????????? ???????????? ???
/*
            REQUEST_GALLERY_TAKE -> {
                // Uri
                if (resultCode == Activity.RESULT_OK) {
                    uriPhoto = data?.data
                    imageView.setImageURI(uriPhoto) // handle chosen image
                    imgFileName = "IMAGE_" + timestamp + "_.jpg"
                }
            }

*/
            REQUEST_GALLERY_TAKE -> {
                if(data == null) {
                    Toast.makeText(applicationContext,"???????????? ???????????? ???????????????.", Toast.LENGTH_LONG).show()
                } else {
                    imageContainer.visibility = View.VISIBLE
                    clipData = data.clipData
                    if(data.clipData == null) {
                        imageView.visibility = View.VISIBLE
                        uriList[0] = data.data
                        imgNameList[0] = "IMAGE_" + timestamp + "_.jpg"
                        imageView.setImageURI(uriList[0])
                    } else {
                        for (i in 0 until clipData!!.itemCount) {
                            uriList[i] = clipData!!.getItemAt(i).uri
                            if (i == 0) {
                                imageView.visibility = View.VISIBLE
                                imgNameList[i] = "IMAGE_" + timestamp + "_.jpg"
                                imageView.setImageURI(uriList[i])
                            } else if (i == 1) {
                                imageView2.visibility = View.VISIBLE
                                imgNameList[i] = "IMAGE_" + timestamp + "-" + (i+1) + "_.jpg"
                                imageView2.setImageURI(uriList[i])
                            } else if (i == 2) {
                                imageView3.visibility = View.VISIBLE
                                imgNameList[i] = "IMAGE_" + timestamp + "-" + (i+1) + "_.jpg"
                                imageView3.setImageURI(uriList[i])
                            }
                        }
                    }
                }
                Toast.makeText(this, "????????? ?????? ???????????? ????????? ??? ????????????.", Toast.LENGTH_SHORT).show()
                val labels = ImageAnnotator.batchAnnotateImages(applicationContext, uriList.toList())
                content.text.append(labels.joinToString(" #", " #"))
            }
        }


    }


    private fun imageUpload() {
        var postDTO : PostDTO? = null

        val progressDialog : ProgressDialog = ProgressDialog(this)

        // ?????????????????? ????????????
        imgFileName = "IMAGE_" + timestamp + "_.jpg"

        storage = FirebaseStorage.getInstance()
        val db = FirebaseFirestore.getInstance()

        if (uriList[1] == null) {
            var riversRef = storage!!.reference.child("post").child(imgNameList[0]!!)
            riversRef.putFile(uriList[0]!!)
                .addOnSuccessListener {

                    riversRef.downloadUrl.addOnSuccessListener { uri ->
                        imageView.visibility = View.VISIBLE
                        db.collection("user_information")
                            .whereEqualTo("email", firebaseAuth!!.currentUser.email).get()
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    for (dc in it.result!!.documents) {
                                        postDTO = dc.toObject(PostDTO::class.java)
                                        break
                                    }
                                }
                            }
                    }
                }

        }
        else {
            for (i in 0 until clipData!!.itemCount) {
                // url ?????? ?????? ??????!
                var riversRef = storage!!.reference.child("post").child(imgNameList[i]!!)
                //riversRef.putFile(uriPhoto!!)
                riversRef.putFile(uriList[i]!!)
                    .addOnSuccessListener {

                        riversRef.downloadUrl.addOnSuccessListener { uri ->
                            db.collection("user_information")
                                .whereEqualTo("email", firebaseAuth!!.currentUser.email).get()
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        for (dc in it.result!!.documents) {
                                            postDTO = dc.toObject(PostDTO::class.java)
                                            break
                                        }
                                    }
                                }
                        }
                    }

            }
        }
    //    Thread.sleep(10000)

    }

    data class Movie(
        val name: String,
        val year: Int
    ) {
        override fun toString(): String {
            return name
        }
    }


}