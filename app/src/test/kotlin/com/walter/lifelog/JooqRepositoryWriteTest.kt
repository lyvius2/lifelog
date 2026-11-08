package com.walter.lifelog

import com.walter.lifelog.blog.entity.Post
import com.walter.lifelog.blog.entity.PostTag
import com.walter.lifelog.blog.entity.code.PostStatus
import com.walter.lifelog.blog.repository.PostTagsRepository
import com.walter.lifelog.blog.repository.PostsRepository
import com.walter.lifelog.photo.entity.Photo
import com.walter.lifelog.photo.entity.PhotoTag
import com.walter.lifelog.photo.repository.PhotoTagsRepository
import com.walter.lifelog.photo.repository.PhotosRepository
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

/**
 * JPA 를 걷어내고 jOOQ 로 다시 쓴 리포지토리의 **쓰기 경로**를 실제 DB(H2) 에 대고
 * 확인합니다.
 *
 * 읽기는 화면과 API 로 금방 드러나지만, insert 의 생성 키 수신, update 가 건드리는
 * 컬럼, 멀티 insert, 그리고 Hibernate 의 @CreationTimestamp /
 * @UpdateTimestamp 를 대신하는 타임스탬프 설정은 조용히 틀릴 수 있습니다.
 *
 * **이 테스트가 잡지 못하는 것이 있습니다.** H2 와 MySQL 은 생성 키를 받는 방식이
 * 다릅니다. H2 는 `SELECT ... FROM FINAL TABLE (INSERT ...)` 로 에뮬레이션하고
 * MySQL 은 JDBC getGeneratedKeys 를 씁니다. 실제로 jOOQ 의 `returning(POST_SEQ)` 는
 * 이 테스트를 통과하면서 MySQL 에서는 null 을 돌려주고 있었습니다 — 코드 생성 없이
 * 문자열로 선언한 필드는 jOOQ 가 identity 인지 알지 못하기 때문입니다. 그래서
 * [com.walter.lifelog.shared.util.GeneratedKeyHelper] 로 JDBC 표준 경로를 직접
 * 씁니다. **생성 키나 dialect 에 민감한 부분을 건드렸다면 MySQL 에 대고도
 * 확인하십시오.**
 */
@SpringBootTest
@DisplayName("jOOQ 리포지토리 쓰기 경로 테스트")
class JooqRepositoryWriteTest {

    @Autowired private lateinit var dsl: DSLContext
    @Autowired private lateinit var postsRepository: PostsRepository
    @Autowired private lateinit var postTagsRepository: PostTagsRepository
    @Autowired private lateinit var photosRepository: PhotosRepository
    @Autowired private lateinit var photoTagsRepository: PhotoTagsRepository

    private companion object {
        /** FK 가 걸려 있어 테스트에서 미리 심어 두는 카테고리 키입니다. */
        const val CATEGORY_SEQ = 1L
    }

    /**
     * H2 는 DB_CLOSE_DELAY=-1 로 테스트 JVM 전체가 한 DB 를 공유하므로 매번 비웁니다.
     * posts / photos 는 각 카테고리 테이블에 FK 가 걸려 있어 카테고리를 먼저 심습니다.
     */
    @BeforeEach
    fun cleanUp() {
        dsl.deleteFrom(DSL.table("posts_tags")).execute()
        dsl.deleteFrom(DSL.table("photos_tags")).execute()
        dsl.deleteFrom(DSL.table("posts")).execute()
        dsl.deleteFrom(DSL.table("photos")).execute()

        if (dsl.fetchCount(DSL.table("categories")) == 0) {
            dsl.insertInto(DSL.table("categories"))
                .set(DSL.field("category_seq", Long::class.java), CATEGORY_SEQ)
                .set(DSL.field("category_name", String::class.java), "개발")
                .set(DSL.field("slug", String::class.java), "dev")
                .set(DSL.field("display_order", Int::class.java), 1)
                .set(DSL.field("is_active", Boolean::class.java), true)
                .set(DSL.field("created_at", LocalDateTime::class.java), LocalDateTime.now())
                .set(DSL.field("updated_at", LocalDateTime::class.java), LocalDateTime.now())
                .execute()
        }
        if (dsl.fetchCount(DSL.table("photos_categories")) == 0) {
            dsl.insertInto(DSL.table("photos_categories"))
                .set(DSL.field("category_seq", Long::class.java), CATEGORY_SEQ)
                .set(DSL.field("category_name", String::class.java), "풍경")
                .set(DSL.field("icon", String::class.java), "🏞️")
                .set(DSL.field("is_active", Boolean::class.java), true)
                .set(DSL.field("created_at", LocalDateTime::class.java), LocalDateTime.now())
                .set(DSL.field("updated_at", LocalDateTime::class.java), LocalDateTime.now())
                .execute()
        }
    }

    private fun createPost(seq: Long? = null, title: String = "제목") = Post(
        postSeq = seq,
        userSeq = 1L,
        categorySeq = null,
        title = title,
        slug = "slug-$title",
        summary = "요약",
        content = "<p>본문</p>",
        markdownContent = "본문",
        status = PostStatus.PUBLISHED,
        viewCount = 7,
        publishedAt = LocalDateTime.now().withNano(0),
    )

    private fun createPhoto(title: String = "사진") = Photo(
        userSeq = 1L,
        title = title,
        caption = "설명",
        imageUrl = "/lifelog/$title.jpg",
        thumbnailUrl = "/lifelog/thumb/$title.jpg",
        shotAt = LocalDateTime.of(2020, 5, 1, 12, 0),
    )

    @Test
    @DisplayName("save - postSeq 가 없으면 insert 하고 생성된 키와 타임스탬프를 채워 돌려준다")
    fun save_newPost_insertsAndReturnsGeneratedKey() {
        // given
        val post = createPost()

        // when
        val saved = postsRepository.save(post)

        // then
        val postSeq = saved.postSeq
        assertThat(postSeq).isNotNull()
        assertThat(saved.createdAt).isNotNull()
        assertThat(saved.updatedAt).isNotNull()

        val reloaded = postsRepository.findByPostSeq(postSeq!!)
        assertThat(reloaded).isNotNull
        assertThat(reloaded!!.title).isEqualTo("제목")
        assertThat(reloaded.slug).isEqualTo("slug-제목")
        assertThat(reloaded.status).isEqualTo(PostStatus.PUBLISHED)
        assertThat(reloaded.viewCount).isEqualTo(7)
        assertThat(reloaded.markdownContent).isEqualTo("본문")
        // 카테고리가 없는 글이므로 조인 결과는 null 입니다
        assertThat(reloaded.category).isNull()
    }

    @Test
    @DisplayName("save - postSeq 가 있으면 update 하고 created_at 은 그대로 둔다")
    fun save_existingPost_updatesAndKeepsCreatedAt() {
        // given
        val inserted = postsRepository.save(createPost())
        val postSeq = inserted.postSeq!!
        val originalCreatedAt = postsRepository.findByPostSeq(postSeq)!!.createdAt

        // when
        postsRepository.save(inserted.copy(title = "바뀐 제목", viewCount = 99))

        // then
        val reloaded = postsRepository.findByPostSeq(postSeq)!!
        assertThat(reloaded.title).isEqualTo("바뀐 제목")
        assertThat(reloaded.viewCount).isEqualTo(99)
        assertThat(reloaded.createdAt).isEqualTo(originalCreatedAt)
    }

    @Test
    @DisplayName("findBySlug - slug 로 찾고 조인한 카테고리를 채운다")
    fun findBySlug_withCategory_populatesCategory() {
        // given
        postsRepository.save(createPost(title = "슬러그 조회").copy(categorySeq = CATEGORY_SEQ))

        // when
        val found = postsRepository.findBySlug("slug-슬러그 조회")

        // then
        assertThat(found).isNotNull
        assertThat(found!!.title).isEqualTo("슬러그 조회")
        assertThat(found.categorySeq).isEqualTo(CATEGORY_SEQ)
        val category = found.category
        assertThat(category).isNotNull
        assertThat(category!!.categoryName).isEqualTo("개발")
        assertThat(category.slug).isEqualTo("dev")
        assertThat(postsRepository.findBySlug("없는-슬러그")).isNull()
    }

    @Test
    @DisplayName("findViewCount - 조회수만 읽는다")
    fun findViewCount_existingPost_returnsOnlyViewCount() {
        // given
        val inserted = postsRepository.save(createPost())

        // when
        val viewCount = postsRepository.findViewCount(inserted.postSeq!!)

        // then
        assertThat(viewCount).isEqualTo(7)
        assertThat(postsRepository.findViewCount(999_999L)).isNull()
    }

    @Test
    @DisplayName("archivePost - status 를 ARCHIVED 로 바꾸고 갱신 행 수를 돌려준다")
    fun archivePost_existingPost_marksArchived() {
        // given
        val postSeq = postsRepository.save(createPost()).postSeq!!

        // when
        val updated = postsRepository.archivePost(postSeq, LocalDateTime.now())

        // then
        assertThat(updated).isEqualTo(1)
        assertThat(postsRepository.findByPostSeq(postSeq)!!.status).isEqualTo(PostStatus.ARCHIVED)
        assertThat(postsRepository.archivePost(999_999L, LocalDateTime.now())).isZero()
    }

    @Test
    @DisplayName("findPrevPostInfo / findNextPostInfo - 같은 카테고리의 앞뒤 발행글을 찾는다")
    fun findNeighbourInfo_publishedPosts_findsPrevAndNext() {
        // given
        val base = LocalDateTime.of(2026, 1, 10, 0, 0)
        fun insert(title: String, at: LocalDateTime) = postsRepository.save(
            createPost(title = title).copy(categorySeq = CATEGORY_SEQ, publishedAt = at)
        )
        val older = insert("오래된 글", base.minusDays(2))
        insert("기준 글", base)
        val newer = insert("새 글", base.plusDays(2))

        // when
        val prev = postsRepository.findPrevPostInfo(CATEGORY_SEQ, base)
        val next = postsRepository.findNextPostInfo(CATEGORY_SEQ, base)

        // then
        assertThat(prev?.postSeq).isEqualTo(older.postSeq)
        assertThat(prev?.title).isEqualTo("오래된 글")
        assertThat(next?.postSeq).isEqualTo(newer.postSeq)
        assertThat(next?.title).isEqualTo("새 글")
    }

    @Test
    @DisplayName("saveAll - 태그를 멀티 insert 하고 findTagsByPostSeq 가 tag_seq 순으로 읽는다")
    fun saveAll_tags_insertsAllInOrder() {
        // given
        val postSeq = postsRepository.save(createPost()).postSeq!!
        val tags = listOf(
            PostTag(postSeq = postSeq, tagSeq = 0, tag = "jOOQ"),
            PostTag(postSeq = postSeq, tagSeq = 1, tag = "Kotlin"),
            PostTag(postSeq = postSeq, tagSeq = 2, tag = "Spring"),
        )

        // when
        val saved = postTagsRepository.saveAll(tags)

        // then
        assertThat(saved).hasSize(3)
        assertThat(saved.map { it.createdAt }).doesNotContainNull()
        assertThat(postTagsRepository.findTagsByPostSeq(postSeq))
            .containsExactly("jOOQ", "Kotlin", "Spring")

        // 빈 목록은 쿼리를 날리지 않고 그대로 끝납니다
        assertThat(postTagsRepository.saveAll(emptyList())).isEmpty()
    }

    @Test
    @DisplayName("deleteByPostSeq - 해당 글의 태그만 지운다")
    fun deleteByPostSeq_onePost_deletesOnlyItsTags() {
        // given
        val keepSeq = postsRepository.save(createPost(title = "남길 글")).postSeq!!
        val dropSeq = postsRepository.save(createPost(title = "지울 글")).postSeq!!
        postTagsRepository.saveAll(listOf(PostTag(keepSeq, 0, "남김")))
        postTagsRepository.saveAll(listOf(PostTag(dropSeq, 0, "지움")))

        // when
        val deleted = postTagsRepository.deleteByPostSeq(dropSeq)

        // then
        assertThat(deleted).isEqualTo(1)
        assertThat(postTagsRepository.findTagsByPostSeq(dropSeq)).isEmpty()
        assertThat(postTagsRepository.findTagsByPostSeq(keepSeq)).containsExactly("남김")
    }

    @Test
    @DisplayName("insert - 사진을 넣고 생성된 photo_seq 를 돌려준다")
    fun insert_photo_returnsGeneratedSeq() {
        // when
        val photoSeq = photosRepository.insert(createPhoto())

        // then
        assertThat(photoSeq).isPositive()
    }

    @Test
    @DisplayName("updateLikeCount / updateActive - 지정한 컬럼만 바꾸고 나머지는 보존한다")
    fun updateColumns_photo_changesOnlyTarget() {
        // given
        val photoSeq = photosRepository.insert(createPhoto())
        val title = DSL.field("photos.title", String::class.java)
        val likeCount = DSL.field("photos.like_count", Int::class.java)
        val isActive = DSL.field("photos.is_active", Boolean::class.java)

        // when
        assertThat(photosRepository.updateLikeCount(photoSeq, 42)).isEqualTo(1)
        assertThat(photosRepository.updateActive(photoSeq, false)).isEqualTo(1)

        // then
        val row = dsl.select(title, likeCount, isActive)
            .from(DSL.table("photos"))
            .where(DSL.field("photos.photo_seq", Long::class.java).eq(photoSeq))
            .fetchOne()!!
        assertThat(row.get(title)).isEqualTo("사진")
        assertThat(row.get(likeCount)).isEqualTo(42)
        assertThat(row.get(isActive)).isFalse()

        // 없는 사진이면 0 건입니다
        assertThat(photosRepository.updateLikeCount(999_999L, 1)).isZero()
    }

    @Test
    @DisplayName("findShotPeriod - 활성 사진의 촬영 연도 최소·최대를 집계 한 번으로 구한다")
    fun findShotPeriod_activePhotos_returnsMinMaxYear() {
        // given
        photosRepository.insert(createPhoto("이른 사진").copy(shotAt = LocalDateTime.of(2015, 3, 1, 9, 0)))
        photosRepository.insert(createPhoto("늦은 사진").copy(shotAt = LocalDateTime.of(2026, 7, 1, 9, 0)))
        // 비활성 사진은 집계에서 빠집니다
        val inactiveSeq = photosRepository.insert(createPhoto("비활성").copy(shotAt = LocalDateTime.of(2030, 1, 1, 9, 0)))
        photosRepository.updateActive(inactiveSeq, false)

        // when
        val period = photosRepository.findShotPeriod()

        // then
        assertThat(period.minYear).isEqualTo(2015)
        assertThat(period.maxYear).isEqualTo(2026)
    }

    @Test
    @DisplayName("findShotPeriod - 사진이 없으면 연도가 null 이다")
    fun findShotPeriod_noPhotos_returnsNulls() {
        // when
        val period = photosRepository.findShotPeriod()

        // then
        assertThat(period.minYear).isNull()
        assertThat(period.maxYear).isNull()
    }

    @Test
    @DisplayName("photos_tags - 멀티 insert 와 삭제가 photo_seq 단위로 동작한다")
    fun photoTags_multiInsertAndDelete_scopedByPhoto() {
        // given
        val photoSeq = photosRepository.insert(createPhoto())
        photoTagsRepository.saveAll(
            listOf(PhotoTag(photoSeq, 0, "풍경"), PhotoTag(photoSeq, 1, "여행"))
        )

        // when
        val tags = photoTagsRepository.findByPhotoSeq(photoSeq)

        // then
        assertThat(tags.map { it.tag }).containsExactly("풍경", "여행")
        assertThat(tags.map { it.createdAt }).doesNotContainNull()
        assertThat(photoTagsRepository.deleteByPhotoSeq(photoSeq)).isEqualTo(2)
        assertThat(photoTagsRepository.findByPhotoSeq(photoSeq)).isEmpty()
    }
}
