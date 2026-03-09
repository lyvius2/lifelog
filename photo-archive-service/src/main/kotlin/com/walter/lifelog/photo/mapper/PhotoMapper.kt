package com.walter.lifelog.photo.mapper

import com.google.api.services.drive.model.File
import com.walter.lifelog.photo.dto.UploadRequest
import com.walter.lifelog.photo.entity.Photo
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants
import org.mapstruct.Named
import java.math.BigDecimal

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PhotoMapper {

    @Mapping(target = "userSeq", source = "userSeq")
    @Mapping(target = "title", source = "uploadRequest.title")
    @Mapping(target = "caption", source = "uploadRequest.caption")
    @Mapping(target = "categorySeq", source = "uploadRequest.categorySeq")
    @Mapping(target = "exifMaker", source = "uploadRequest.maker")
    @Mapping(target = "exifModel", source = "uploadRequest.model")
    @Mapping(target = "exifLens", source = "uploadRequest.lens")
    @Mapping(target = "exifAperture", source = "uploadRequest.aperture")
    @Mapping(target = "exifShutter", source = "uploadRequest.shutter")
    @Mapping(target = "exifIso", source = "uploadRequest.iso")
    @Mapping(target = "exifFocalLength", source = "uploadRequest.focalLength")
    @Mapping(target = "exifFlash", source = "uploadRequest.flash")
    @Mapping(target = "gpsLatitude", source = "uploadRequest.latitude", qualifiedByName = ["doubleToBigDecimal"])
    @Mapping(target = "gpsLongitude", source = "uploadRequest.longitude", qualifiedByName = ["doubleToBigDecimal"])
    @Mapping(target = "shotAt", source = "uploadRequest.shotAt")
    @Mapping(target = "photoSeq", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "imageUrl", expression = "java(\"/\" + folderPath + \"/\" + driveFile.getName())")
    @Mapping(target = "thumbnailUrl", source = "driveFile.thumbnailLink")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    fun toEntity(uploadRequest: UploadRequest, driveFile: File, userSeq: Long, folderPath: String): Photo

    @Named("longToString")
    fun longToString(value: Long?): String? = value?.toString()

    @Named("doubleToBigDecimal")
    fun doubleToBigDecimal(value: Double?): BigDecimal? = value?.let { BigDecimal.valueOf(it) }
}