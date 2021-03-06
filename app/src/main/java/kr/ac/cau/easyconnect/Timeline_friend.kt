package kr.ac.cau.easyconnect

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.time.LocalDateTime

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

class Timeline_friend : Fragment() {
    // TODO: Rename and change types of parameters
    var storage: FirebaseStorage? = null
    var firebaseAuth: FirebaseAuth? = null
    var db: FirebaseFirestore? = null

    private var friendEmail : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            friendEmail = it.getString("email")
        }

        storage = FirebaseStorage.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view : View = inflater.inflate(R.layout.fragment_timeline_friend, container, false)
        val timelineView : RecyclerView = view.findViewById(R.id.recycler_photocardView)
        val context: Context = view.context
        val lm = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        timelineView.layoutManager = lm
        timelineView.adapter = PhotoCardAdapter()

        return view
    }

    inner class PhotoCardAdapter() : RecyclerView.Adapter<PhotoCardAdapter.PhotoCardViewHolder>(){
        // ????????? ??? ????????? ?????? ??? ????????? ???????????? ????????? ???????????? ?????? ???
        var arrayPostDTO : ArrayList<PostDTO> = arrayListOf()

        init{
            // ???????????????????????? ?????? ?????? ?????? ?????????????????? ????????? ????????????.
            // ?????? ???????????? ????????? ???????????? arrayPostDTO ??? ????????? ???
            db!!.collection("post").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                arrayPostDTO.clear()
                // post_information??? ?????? ????????? ????????? ???????????? ??? ???????????? ?????? ????????? ??? ????????? ????????? ???????????????
                for (snapshot in querySnapshot!!.documents.reversed()) {
                    // ????????? ???????????? ?????? ???????????? ?????? ?????? ??? ??? ??????! ????????? ???????????? ????????????????????? ????????????
                    var post = snapshot.toObject(PostDTO::class.java)

                    // ???????????? ???????????? ??? ???? ????????? ?????? ????????? ????????? ????????? ?????? ???????????? ???????????????..
                    if (post!!.name == friendEmail) {
                        // ????????? ????????? ????????? ???????????? ?????? ????????????!
                        arrayPostDTO!!.add(post!!)
                    }
//                    arrayPostDTO!!.add(post!!)
                }
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int{
            return arrayPostDTO.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : PhotoCardViewHolder{
            val inflatedView =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.photocard_item_list, parent, false)
            return PhotoCardViewHolder(inflatedView)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBindViewHolder(holder: PhotoCardViewHolder, position: Int){
            val postDTO = arrayPostDTO[position]
            holder.apply{
                bind(postDTO)
            }
        }

        inner class PhotoCardViewHolder(v: View) : RecyclerView.ViewHolder(v){
            var view : View = v
            var photoCardLayout: View = view.findViewById(R.id.layout_photoCard_item)
            var photoOfDetail: ImageView = view.findViewById(R.id.photo_of_post)
            var nameOfDetail: TextView = view.findViewById(R.id.name_of_post)
            var dateOfDetail: TextView = view.findViewById(R.id.date_of_post)
            var alarmNew : ImageView = view.findViewById(R.id.new_alarm)

            // ??? ??? ??? ??????????????? ????????????!
            @RequiresApi(Build.VERSION_CODES.O)
            fun bind(item: PostDTO){
                var now : String = LocalDateTime.now().toString()
                var lastDate : String? = item.modified

                var parsing_now = now.split("T")
                var parsing_now_date = parsing_now[0].split("-")

                var parsing_lastDate = lastDate!!.split("T")
                var parsing_lastDate_date = parsing_lastDate[0].split("-")

                var detailDateFromNow_year : Int = parsing_now_date[0].toInt() - parsing_lastDate_date[0].toInt()
                var detailDateFromNow_month : Int = parsing_now_date[1].toInt() - parsing_lastDate_date[1].toInt()
                var detailDateFromNow_day : Int = parsing_now_date[2].toInt() - parsing_lastDate_date[2].toInt()

                if(!item.imageOfDetail.isNullOrEmpty()){
                    val storageReference = storage!!.reference
                    storageReference.child("post/" + item.imageOfDetail).downloadUrl.addOnSuccessListener {
                        Glide.with(photoCardLayout /* context */)
                            .load(it)
                            .into(photoOfDetail)
                    }
                }
                // ?????? ??????
                if(detailDateFromNow_year > 0){
                    // ?????? ?????? ??? ?????? ?????????
                    alarmNew.setBackgroundResource(R.drawable.drawable_empty)
                    dateOfDetail.setText(parsing_lastDate[0])
                }else{
                    // ?????? ?????????
                    if(detailDateFromNow_month == 0){
                        // ?????? ???
                        if(detailDateFromNow_day == 0){
                            alarmNew.setBackgroundResource(R.drawable.drawable_new_post)
                            dateOfDetail.setText("??????")
                        }else if(detailDateFromNow_day > 3){
                            alarmNew.setBackgroundResource(R.drawable.drawable_empty)
                            dateOfDetail.setText(parsing_lastDate[0])
                        }else{
                            alarmNew.setBackgroundResource(R.drawable.drawable_empty)
                            dateOfDetail.setText(detailDateFromNow_day.toString() + "??? ???")
                        }
                    }else{
                        alarmNew.setBackgroundResource(R.drawable.drawable_empty)
                        dateOfDetail.setText(parsing_lastDate[0])
                    }
                }

                nameOfDetail.setText(item.content)

                photoCardLayout.setOnClickListener{
                    val intentDetail = Intent(view.context, DetailMainActivity::class.java).apply{
                        val data = item.name + " " + item.modified
                        val flag = "friend"
                        putExtra("data", data)
                        putExtra("flag", flag)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intentDetail)
                }
            }

        }
    }
}