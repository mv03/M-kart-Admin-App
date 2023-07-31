package com.example.m_kartadminapp.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.media.MediaRouter.UserRouteInfo
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ArrayRes
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.m_kartadminapp.R
import com.example.m_kartadminapp.adapter.AddProductImageAdapter
import com.example.m_kartadminapp.databinding.FragmentAddProductBinding
import com.example.m_kartadminapp.databinding.FragmentProductBinding
import com.example.m_kartadminapp.model.AddProductModel
import com.example.m_kartadminapp.model.CategoryModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_slider.*
import java.util.*
import kotlin.collections.ArrayList


class AddProductFragment : Fragment() {

    private lateinit var binding: FragmentAddProductBinding
    private lateinit var list:ArrayList<Uri>
    private lateinit var listImages:ArrayList<String>
    private lateinit var adapter: AddProductImageAdapter
    private var coverImage:Uri?=null
    private lateinit var dialog:Dialog
    private var coverImageURl:String?=""
    private lateinit var categoryList:ArrayList<String>



    private var launchGalleryActivity=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode== Activity.RESULT_OK){
            coverImage=it.data!!.data
            binding.productCoverImg.setImageURI(coverImage)
            binding.productCoverImg.setVisibility(View.VISIBLE)
        }

    }

    private var launchProductActivity=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode== Activity.RESULT_OK){
            val imageUrl=it.data!!.data
            list.add(imageUrl!!)
            adapter.notifyDataSetChanged()

        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentAddProductBinding.inflate(layoutInflater)

        list= ArrayList()
        listImages= ArrayList()

        dialog= Dialog(requireContext())
        dialog.setContentView(R.layout.progress_layout)
        dialog.setCancelable(false)

        binding.selectCoverImg.setOnClickListener{
            val intent= Intent("android.intent.action.GET_CONTENT")
            intent.type="image/*"
            launchGalleryActivity.launch(intent)
        }

        binding.productImgButton.setOnClickListener{
            val intent= Intent("android.intent.action.GET_CONTENT")
            intent.type="image/*"
            launchProductActivity.launch(intent)
        }


        setProductCategory()

        adapter= AddProductImageAdapter(list)
        binding.productImgRecycler.adapter=adapter

        binding.submitBtn.setOnClickListener {
            validateData()
        }

        return binding.root
    }

    private fun validateData() {
        if(binding.productName.text.toString().isEmpty()){
            binding.productName.requestFocus()
            binding.productName.error="Empty "
        }else  if(binding.productDescription.text.toString().isEmpty()){
            binding.productDescription.requestFocus()
            binding.productDescription.error="Empty "
        }else  if(binding.productMRP.text.toString().isEmpty()){
            binding.productMRP.requestFocus()
            binding.productMRP.error="Empty "
        }else  if(binding.productSP.text.toString().isEmpty()){
            binding.productSP.requestFocus()
            binding.productSP.error="Empty "
        }else if(coverImage==null){
            Toast.makeText(requireContext(),"Please select cover image",Toast.LENGTH_LONG).show()
        }else if(list.size<1){
            Toast.makeText(requireContext(),"Please select product image",Toast.LENGTH_LONG).show()
        }else{
            uploadImage()
        }
    }
    private fun setProductCategory(){
        categoryList= ArrayList()
        Firebase.firestore.collection("categories").get().addOnSuccessListener {
            categoryList.clear()
            for(doc in it.documents){
                val data=doc.toObject(CategoryModel::class.java)
                categoryList.add(data!!.cat!!)

            }
            categoryList.add(0,"Select Category")

            val arrayAdapter=ArrayAdapter(requireContext(),R.layout.dropdown_item,categoryList)
            binding.productCategoryDropdown.adapter=arrayAdapter
        }
    }




    private fun uploadImage() {
        dialog.show()

        val fileName= UUID.randomUUID().toString()+".jpg"

        val refStorage= FirebaseStorage.getInstance().reference.child("products/$fileName")
        refStorage.putFile(coverImage!!)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener {image->
                    coverImageURl=image.toString()

                    uploadProductImage()
                }
            }
            .addOnFailureListener{
                dialog.dismiss()
                Toast.makeText(requireContext(), "Something went wrong with storage", Toast.LENGTH_LONG).show()
            }
    }

    private var i=0
    private fun uploadProductImage() {
        dialog.show()

        val fileName= UUID.randomUUID().toString()+".jpg"

        val refStorage= FirebaseStorage.getInstance().reference.child("products/$fileName")
        refStorage.putFile(list[i]!!)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener {image->
                    listImages.add(image!!.toString())
                    if(list.size==listImages.size){
                        storeData()
                    }else{
                        i+=1
                        uploadProductImage()
                    }

                }
            }
            .addOnFailureListener{
                dialog.dismiss()
                Toast.makeText(requireContext(), "Something went wrong with storage", Toast.LENGTH_LONG).show()
            }
    }

    private fun storeData() {
        val db=Firebase.firestore.collection("products")
        val key=db.document().id

        val data=AddProductModel(
            binding.productName.text.toString(),
            binding.productDescription.text.toString(),
            coverImageURl.toString(),
            categoryList[binding.productCategoryDropdown.selectedItemPosition],
            key,
            binding.productMRP.text.toString(),
            binding.productSP.text.toString(),
            listImages
        )
        db.document(key).set(data).addOnSuccessListener {
            dialog.dismiss()
            Toast.makeText(requireContext(),"Product Added",Toast.LENGTH_LONG).show()
            binding.productName.text=null
            binding.productDescription.text=null
            binding.productMRP.text=null
            binding.productSP.text=null
        }.addOnFailureListener {
            dialog.dismiss()
            Toast.makeText(requireContext(),"Something went wrong",Toast.LENGTH_LONG).show()
        }
    }
}


