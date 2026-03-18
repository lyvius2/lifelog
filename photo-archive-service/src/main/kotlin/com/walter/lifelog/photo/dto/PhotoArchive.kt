package com.walter.lifelog.photo.dto

import com.walter.lifelog.shared.paging.PageResponse

data class PhotoArchive(
    var categories: List<PhotoCategoryResponse>,
    var archive: PageResponse<PhotoSearchResponse>,
    var period: PhotoShotPeriod,
)
