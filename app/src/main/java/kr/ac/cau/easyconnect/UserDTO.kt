package kr.ac.cau.easyconnect

// Firebase-firestore에 저장될 user 데이터 클래스 정의
data class UserDTO(var email:String? = null, var password:String? = null, var name:String? = null, var phoneNumber:String? = null)