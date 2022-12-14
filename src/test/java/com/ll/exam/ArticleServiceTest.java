package com.ll.exam;

import com.ll.exam.article.dto.ArticleDto;
import com.ll.exam.article.service.ArticleService;
import com.ll.exam.mymap.MyMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArticleServiceTest {
    private MyMap myMap;
    private ArticleService articleService;
    private static final int TEST_DATA_SIZE = 100;

    public ArticleServiceTest() {
        myMap = Container.getObj(MyMap.class);
        articleService = Container.getObj(ArticleService.class);
    }



    // @BeforeAll 붙인 아래 메서드는
    @BeforeAll
    public void BeforeAll() {
        // 모든 DB 처리시에, 처리되는 SQL을 콘솔에 출력
        myMap.setDevMode(true);
    }

    /*
    @BeforeEach 를 통해 매 Test 마다 테이블 내용을 비우고, 다시 테스트 케이스를 채우는 과정을 수행한다.
    -> 각각의 테스트케이스가 독립적인 환경에서 실행될 수 있도록 만들어줌.
     */
    @BeforeEach
    public void beforeEach() {
        truncateArticleTable(); // DELETE FROM article; // 보다 TRUNCATE article; 로 삭제하는게 더 깔끔하고 흔적이 남지 않는다.
        makeArticleTestData();   // 테스트에 필요한 테스트데이터 3개 추가.
    }

    private void makeArticleTestData() {
        MyMap myMap = Container.getObj(MyMap.class);

        IntStream.rangeClosed(1, TEST_DATA_SIZE).forEach(no -> {
            boolean isBlind = no >= 11 && no <= 20;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            myMap.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        MyMap myMap = Container.getObj(MyMap.class);
        myMap.run("TRUNCATE article");   // 테이블을 깔끔하게 지워준다.
    }


    @Test
    public void 존재한다() {

        assertThat(articleService).isNotNull();
    }

    @Test
    public void getArticles() {

        List<ArticleDto> articleDtoList = articleService.getArticles();
        assertThat(articleDtoList.size()).isEqualTo(TEST_DATA_SIZE);
    }

    @Test
    public void getArticleById() {
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1L);
        assertThat(articleDto.getTitle()).isEqualTo("제목1");
        assertThat(articleDto.getBody()).isEqualTo("내용1");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isFalse();
    }

    @Test
    public void getArticlesCount() {
        // selectLong 메서드 이용
        long articlesCount = articleService.getArticlesCount();

        assertThat(articlesCount).isEqualTo(TEST_DATA_SIZE);
    }

    @Test
    public void write() {    // 데이터 추가 코드.

        long newArticleId = articleService.write("제목 new", "내용 new", false);

        ArticleDto articleDto = articleService.getArticleById(newArticleId);

        assertThat(articleDto.getId()).isEqualTo(newArticleId);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isEqualTo(false);
    }

    @Test
    public void modify() {
        //Ut.sleep(5000);

        articleService.modify(1, "제목 new", "내용 new", true);

        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.isBlind()).isEqualTo(true);

        // DB에서 받아온 게시물 수정날짜와 자바에서 계산한 현재 날짜를 비교하여(초단위)
        // 그것이 1초 이하로 차이가 난다면
        // 수정날짜가 갱신되었다 라고 볼 수 있음
        long diffSeconds = ChronoUnit.SECONDS.between(articleDto.getModifiedDate(), LocalDateTime.now());
        assertThat(diffSeconds).isLessThanOrEqualTo(1L);
    }

    @Test
    public void delete() {

        articleService.deleteById(1);
        ArticleDto articleDto = articleService.getArticleById(1);
        assertThat(articleDto).isEqualTo(null);   // isNull(); 써도 똑같다.
    }

    @Test
    public void 이전_글_가져오기() {
        // 1번 케이스 . 2번글의 이전 글은 1번 글이다. (id로 가져오기.)
        ArticleDto articleDto = articleService.getBeforeId(2);
        assertThat(articleDto.getId()).isEqualTo(1);
        // 2번 케이스. 1번글의 이전 글은 없습니다. (null 이 나와야 함.)
        ArticleDto articleDto1 = articleService.getBeforeId(1);
        assertThat(articleDto1).isNull();

    }
    @Test
    public void 다음_글_가져오기() {
        // 1번 케이스. 2번글의 다음 글은 3번 글입니다.
        ArticleDto articleDto = articleService.getAfterId(2);
        assertThat(articleDto.getId()).isEqualTo(3);

        // 마지막 글의 다음글은 없습니다. (null 이 나와야 함.)
        ArticleDto articleDto1 = articleService.getAfterId(TEST_DATA_SIZE);
        assertThat(articleDto1).isNull();
    }

    @Test
    public void _10번_글의_다음글은_21번글_이다_블라인드_처리가_적용됨() {
        ArticleDto articleDto = articleService.getAfterId(10);
        assertThat(articleDto.getId()).isEqualTo(21);

    }




}