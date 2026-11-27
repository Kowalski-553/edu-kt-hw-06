package com.example.edu_kt_hw_06.library

import com.example.edu_kt_hw_06.data.LibraryRepository

class UserLibrary(repository: LibraryRepository) :
    RoleLibrary,
    BookCatalog by repository,
    BookingService by repository {

    override val catalog: BookCatalog get() = this
}
