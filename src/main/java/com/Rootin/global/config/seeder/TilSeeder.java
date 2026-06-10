package com.Rootin.global.config.seeder;

import com.Rootin.domain.gamification.entity.PointLog;
import com.Rootin.domain.gamification.entity.enums.PointLogReason;
import com.Rootin.domain.gamification.repository.PointLogRepository;
import com.Rootin.domain.garden.entity.Pot;
import com.Rootin.domain.garden.repository.PotRepository;
import com.Rootin.domain.til.entity.Tag;
import com.Rootin.domain.til.entity.Til;
import com.Rootin.domain.til.entity.TilTag;
import com.Rootin.domain.til.repository.TagRepository;
import com.Rootin.domain.til.repository.TilRepository;
import com.Rootin.domain.til.repository.TilTagRepository;
import com.Rootin.domain.user.entity.User;
import com.Rootin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TilSeeder {

    private final TilRepository tilRepository;
    private final TilTagRepository tilTagRepository;
    private final TagRepository tagRepository;
    private final PointLogRepository pointLogRepository;
    private final PotRepository potRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    // ── 태그별 TIL 제목 풀 ────────────────────────────────────────────────────

    private static final List<String> JAVA_TITLES = List.of(
            "Java 제네릭(Generic) 타입 경계 wildcards 정리",
            "equals()와 hashCode() 재정의 규칙",
            "Java Stream API - reduce와 collect 차이점",
            "checked vs unchecked 예외 언제 써야 할까",
            "Java 동시성 - synchronized vs ReentrantLock",
            "Optional 남용하지 않는 법",
            "String vs StringBuilder 성능 차이 실험",
            "Java 17 sealed class 활용 패턴",
            "record 클래스로 DTO 간결하게 만들기",
            "JVM GC 종류와 G1GC 튜닝 포인트",
            "람다와 함수형 인터페이스 직접 만들어보기",
            "instanceof 패턴 매칭으로 타입 분기 정리"
    );

    private static final List<String> SPRING_TITLES = List.of(
            "Spring Bean 생명주기와 초기화 콜백 정리",
            "@Transactional 전파 속성 REQUIRES_NEW vs NESTED",
            "Spring Security Filter Chain 동작 순서",
            "JPA N+1 문제 fetch join으로 해결하기",
            "QueryDSL 동적 쿼리 BooleanBuilder 패턴",
            "@EventListener vs ApplicationEventPublisher 선택 기준",
            "Spring Boot Actuator 운영 환경 설정",
            "JPA Auditing으로 createdAt/updatedAt 자동화",
            "@Async 비동기 처리 주의사항 정리",
            "Spring Cache 추상화 @Cacheable 적용기",
            "Testcontainers로 MySQL 통합 테스트 환경 구축",
            "@ControllerAdvice 전역 예외 처리 설계"
    );

    private static final List<String> REACT_TITLES = List.of(
            "useEffect 의존성 배열 제대로 이해하기",
            "React Query staleTime과 cacheTime 차이",
            "Context API vs Zustand 상태관리 선택 기준",
            "React.memo와 useMemo 언제 써야 효과적인가",
            "커스텀 훅으로 로직 분리하기 - useFetch 만들기",
            "React 18 Concurrent Rendering 개념 정리",
            "Suspense와 ErrorBoundary로 로딩/에러 처리",
            "React Router v6 중첩 라우팅 구조 설계",
            "Intersection Observer로 무한 스크롤 구현",
            "폼 상태관리 React Hook Form vs Formik",
            "TypeScript + React - 제네릭 컴포넌트 패턴",
            "Vite 빌드 최적화 - 코드 스플리팅 전략"
    );

    private static final List<String> ENGLISH_TITLES = List.of(
            "가산·불가산 명사 헷갈리는 케이스 총정리",
            "현재완료 vs 과거시제 - 원어민이 구분하는 법",
            "비즈니스 이메일 writing 패턴 10가지",
            "조동사 should/would/could 뉘앙스 차이",
            "관계대명사 that vs which 선택 규칙",
            "영어 프레젠테이션 오프닝 문장 모음",
            "기술 면접 영어 답변 구조 STAR 기법",
            "자주 틀리는 전치사 in/on/at 구분 정리",
            "영어 독해 속도 올리는 스키밍 전략",
            "수동태를 능동태로 바꿔야 하는 이유"
    );

    private static final List<String> GRAMMAR_TITLES = List.of(
            "가정법 과거·과거완료 구분해서 쓰기",
            "분사구문 만들기 - 주어 일치 규칙",
            "도치 구문 - 부정어 강조할 때 쓰는 패턴",
            "접속사 although vs despite vs in spite of",
            "It ~ that 강조 구문 활용법",
            "관계부사 where/when/why 빠르게 구분하기",
            "간접의문문 어순 실수 없애기",
            "병렬 구조 parallelism 문법 규칙 정리"
    );

    private static final List<String> READING_TITLES = List.of(
            "『클린 코드』 - 함수는 한 가지 일만 해야 한다",
            "『함께 자라기』 - 애자일 회고의 진짜 의미",
            "『미라클 모닝』 - 아침 루틴이 생산성을 바꾸는 이유",
            "『도둑맞은 집중력』 - 딥 워크의 조건들",
            "『사피엔스』 - 인지혁명이 인류를 지배하게 만든 방식",
            "『원칙』 - 레이 달리오의 의사결정 프레임워크",
            "『린 스타트업』 - MVP로 가설 검증하는 법",
            "『아웃라이어』 - 1만 시간의 법칙 다시 읽기",
            "『넛지』 - 선택 설계로 행동을 바꾸는 원리",
            "『데일 카네기 인간관계론』 - 경청이 설득보다 강한 이유"
    );

    private static final List<String> ALGO_TITLES = List.of(
            "이진 탐색 경계 조건 - lo/hi 설정 실수 없애기",
            "DP 점화식 세우는 법 - Top-down vs Bottom-up",
            "BFS와 DFS 선택 기준 정리",
            "슬라이딩 윈도우로 부분 배열 문제 풀기",
            "유니온-파인드 경로 압축 구현",
            "우선순위 큐로 다익스트라 최단경로 구현",
            "투 포인터 패턴 - 정렬된 배열 합 문제",
            "트리 DP - 서브트리 값 누적 계산",
            "LCS(최장 공통 부분 수열) 점화식 유도",
            "비트마스킹으로 부분집합 열거하기",
            "세그먼트 트리 구간 합 쿼리 구현",
            "그리디 증명 - 교환 논법으로 최적성 확인"
    );

    private static final List<String> FITNESS_TITLES = List.of(
            "스쿼트 자세 교정 — 무릎 정렬 포인트 정리",
            "인터벌 트레이닝 30분 루틴 기록",
            "유산소 vs 무산소 운동 비율 실험 후기",
            "운동 전후 스트레칭 루틴 정리",
            "데드리프트 허리 부상 없이 치는 법",
            "3분할 루틴 한 달 후기",
            "홈트 기구 없이 전신 운동하는 법",
            "운동 일지 쓰는 법 — 중량·세트·휴식 기록 방법"
    );

    // ── 태그별 TIL 본문 풀 ────────────────────────────────────────────────────

    private static final Map<String, List<String>> CONTENTS = Map.of(
            "Java", List.of(
                    """
                    ## 오늘 배운 것
                    제네릭의 upper bounded wildcard(`? extends T`)와 lower bounded wildcard(`? super T`)를 드디어 제대로 이해했다.

                    핵심은 **PECS 원칙** — Producer Extends, Consumer Super.
                    - 값을 꺼낼(produce) 때는 `? extends T`
                    - 값을 넣을(consume) 때는 `? super T`

                    ```java
                    // 읽기 전용 → extends
                    public double sum(List<? extends Number> list) { ... }

                    // 쓰기 전용 → super
                    public void addNumbers(List<? super Integer> list) { ... }
                    ```

                    ## 느낀 점
                    제네릭을 그냥 `<T>`로만 쓰다가 wildcard를 처음 보면 왜 두 가지인지 직관적으로 안 잡히는데, PECS로 외우니 금방 정리됐다.
                    """,
                    """
                    ## 오늘 배운 것
                    `equals()`를 재정의할 때 `hashCode()`도 반드시 같이 재정의해야 하는 이유를 제대로 짚었다.

                    HashMap은 버킷을 찾을 때 `hashCode()`를 먼저 쓰고, 같은 버킷 안에서 `equals()`로 비교한다.
                    `hashCode()`만 다르면 같은 객체인데 다른 버킷에 들어가서 get이 null을 반환하는 버그가 생긴다.

                    ```java
                    @Override
                    public int hashCode() {
                        return Objects.hash(id, email);
                    }
                    ```

                    ## 느낀 점
                    Lombok `@EqualsAndHashCode`를 쓰면 편하긴 한데, JPA Entity에 쓸 때는 연관 엔티티를 포함시키면
                    LazyInitializationException 날 수 있어서 주의해야 한다.
                    """
            ),
            "Spring", List.of(
                    """
                    ## 오늘 배운 것
                    `@Transactional(propagation = Propagation.REQUIRES_NEW)`를 잘못 쓰면 트랜잭션이 예상과 다르게 동작한다는 걸 직접 겪었다.

                    - `REQUIRED` : 기존 트랜잭션에 합류. 없으면 새로 생성
                    - `REQUIRES_NEW` : 기존 트랜잭션을 **일시 중단**하고 새 트랜잭션으로 실행

                    문제 상황: 부모 트랜잭션이 롤백돼도 `REQUIRES_NEW` 자식은 이미 커밋된 상태라 데이터 불일치가 생겼다.

                    ```java
                    @Transactional(propagation = Propagation.REQUIRES_NEW)
                    public void saveAuditLog(AuditLog log) {
                        // 부모 롤백과 무관하게 독립 커밋됨
                        auditLogRepository.save(log);
                    }
                    ```

                    ## 느낀 점
                    감사 로그처럼 "부모가 실패해도 반드시 기록해야 하는 것"에는 `REQUIRES_NEW`가 맞지만,
                    일반 비즈니스 로직에서 습관적으로 쓰는 건 위험하다.
                    """,
                    """
                    ## 오늘 배운 것
                    JPA N+1 문제를 fetch join으로 해결하다가 `MultipleBagFetchException`을 만났다.

                    컬렉션을 두 개 이상 fetch join하면 카테시안 곱이 발생해서 Hibernate가 거부한다.
                    해결 방법은 하나만 fetch join하고 나머지는 `@BatchSize`로 처리하는 것.

                    ```java
                    // ❌ 두 컬렉션 동시 fetch join → 예외
                    SELECT p FROM Post p
                    JOIN FETCH p.tags
                    JOIN FETCH p.comments

                    // ✅ 하나만 fetch join + BatchSize
                    @BatchSize(size = 100)
                    @OneToMany(mappedBy = "post")
                    private List<Comment> comments;
                    ```

                    ## 느낀 점
                    `@BatchSize`는 IN 쿼리로 N+1을 줄여주는데, fetch join만큼 완전히 없애지는 못하지만 실용적인 타협안이다.
                    """
            ),
            "React", List.of(
                    """
                    ## 오늘 배운 것
                    `useEffect` 의존성 배열에서 함수를 넣을 때 무한 루프가 생기는 이유를 파악했다.

                    컴포넌트가 렌더링될 때마다 함수 참조가 새로 만들어지기 때문에 의존성 배열이 항상 변경된 것으로 감지된다.

                    ```jsx
                    // ❌ fetchData가 매 렌더에 새로 생성 → 무한 루프
                    const fetchData = () => { ... };
                    useEffect(() => { fetchData(); }, [fetchData]);

                    // ✅ useCallback으로 참조 고정
                    const fetchData = useCallback(() => { ... }, [userId]);
                    useEffect(() => { fetchData(); }, [fetchData]);
                    ```

                    ## 느낀 점
                    ESLint의 `exhaustive-deps` 규칙을 켜두면 의존성 누락을 잡아줘서 도움이 된다.
                    무조건 경고를 끄는 게 아니라 `useCallback`으로 해결하는 게 맞다.
                    """,
                    """
                    ## 오늘 배운 것
                    React Query의 `staleTime`과 `cacheTime`(gcTime)이 다른 개념이라는 걸 명확히 정리했다.

                    - `staleTime` : 데이터를 **신선한 것으로 간주**하는 시간. 이 시간 내에는 refetch 안 함
                    - `gcTime` (구 cacheTime) : 캐시를 **메모리에 유지**하는 시간. 컴포넌트 언마운트 후 기준

                    ```js
                    useQuery({
                      queryKey: ['user'],
                      queryFn: fetchUser,
                      staleTime: 1000 * 60 * 5,   // 5분간 fresh
                      gcTime: 1000 * 60 * 10,      // 10분간 캐시 유지
                    });
                    ```

                    ## 느낀 점
                    목록 페이지에서 상세 페이지 갔다가 돌아올 때 깜빡임이 생기면 `staleTime`을 늘리면 해결된다.
                    API 호출 빈도와 데이터 신선도 사이의 트레이드오프를 팀과 논의해야 할 것 같다.
                    """
            ),
            "영어", List.of(
                    """
                    ## 오늘 배운 것
                    현재완료와 과거시제를 언제 써야 하는지 원어민 감각으로 정리해봤다.

                    핵심 차이:
                    - 과거시제: 과거의 특정 시점 강조 (`I lost my key yesterday.`)
                    - 현재완료: 과거 행위의 **현재 영향** 강조 (`I've lost my key.` → 지금도 없음)

                    자주 헷갈리는 케이스:
                    ```
                    Did you eat lunch? → 점심 먹었어? (일반 질문)
                    Have you eaten lunch? → 점심 먹었어? (지금 배고프냐는 뉘앙스)
                    ```

                    ## 느낀 점
                    영어권 뉴스 기사 읽을 때 의식적으로 시제를 분석하면서 읽으니 감이 잡히기 시작했다.
                    """,
                    """
                    ## 오늘 배운 것
                    비즈니스 이메일에서 자주 쓰는 정중한 요청 표현들을 정리했다.

                    ```
                    I was wondering if you could ~  → ~해 주실 수 있을까요?
                    Would it be possible to ~?       → ~이 가능할까요?
                    I'd appreciate it if ~           → ~해 주시면 감사하겠습니다
                    Please feel free to ~            → 편하게 ~하세요
                    ```

                    마감 언급 방법:
                    ```
                    At your earliest convenience  → 되는 대로 빨리
                    By end of day Friday          → 금요일 업무 종료 전까지
                    ```

                    ## 느낀 점
                    한국어를 직역하면 딱딱하게 들린다. "Please do this"보다 "I was wondering if you could" 같은
                    우회 표현이 훨씬 자연스럽다는 걸 이번에 확실히 익혔다.
                    """
            ),
            "문법", List.of(
                    """
                    ## 오늘 배운 것
                    가정법 과거(`If + 과거형, would + 동사원형`)와 가정법 과거완료(`If + had p.p., would have p.p.`)의 차이를 정리했다.

                    ```
                    가정법 과거 (현재 사실과 반대):
                    If I had more time, I would study harder.
                    (시간이 있다면 더 열심히 공부할 텐데 → 실제로 시간이 없음)

                    가정법 과거완료 (과거 사실과 반대):
                    If I had had more time, I would have studied harder.
                    (시간이 있었더라면 더 열심히 공부했을 텐데 → 실제로 시간이 없었음)
                    ```

                    ## 느낀 점
                    혼합 가정법(`If I had p.p., I would 동사원형`)도 있는데, 이건 과거 원인 + 현재 결과 조합이다.
                    실전에서 이 세 가지를 구분하는 게 꽤 까다롭지만 반복 노출이 답인 것 같다.
                    """,
                    """
                    ## 오늘 배운 것
                    분사구문의 주어 일치 규칙을 확실히 정리했다.

                    분사구문은 주절의 주어와 분사의 의미상 주어가 같을 때 쓴다.

                    ```
                    ✅ Walking down the street, I found a wallet.
                       → I was walking, I found (주어 일치)

                    ❌ Walking down the street, a wallet was found.
                       → wallet이 걷지 않음 → 댕글링 수식어(dangling modifier)
                    ```

                    ## 느낀 점
                    영어 글쓰기에서 이 실수를 많이 한다. 분사구문 쓸 때는 주절 주어를 먼저 확인하는 습관을 들여야겠다.
                    """
            ),
            "독서", List.of(
                    """
                    ## 오늘 읽은 것
                    『클린 코드』 3장 - 함수

                    ## 핵심 내용
                    함수는 **한 가지 일만** 해야 한다. 함수 이름으로 설명할 수 없는 일을 하고 있다면 분리해야 한다.

                    > "함수에서 이상적인 인수 개수는 0개다."

                    인수가 많아질수록 함수 이해가 어렵고 테스트 케이스도 기하급수적으로 늘어난다.
                    플래그 인수는 특히 나쁘다 — 함수가 두 가지 일을 한다는 신호다.

                    ```java
                    // ❌ 플래그 인수
                    render(true);

                    // ✅ 이름으로 의도를 드러내기
                    renderForSuite();
                    renderForSingleTest();
                    ```

                    ## 느낀 점
                    내 코드를 돌아보면 플래그 인수가 꽤 있다. 리팩토링할 때 우선순위로 잡아야겠다.
                    """,
                    """
                    ## 오늘 읽은 것
                    『함께 자라기』 - 애자일 회고 챕터

                    ## 핵심 내용
                    회고의 핵심은 **심리적 안전감**이다. 잘못을 지적하는 자리가 아니라 함께 배우는 자리여야 한다.

                    저자가 강조한 실천 방법:
                    1. 사람이 아닌 **시스템과 프로세스**를 개선 대상으로
                    2. "왜 그랬어?"가 아닌 "어떤 맥락이었어?"로 질문
                    3. 비난 없는 사후 분석(blameless postmortem)

                    ## 느낀 점
                    팀에서 회고를 할 때 침묵이 많았던 이유를 이제 알 것 같다.
                    형식이 있어도 심리적 안전감이 없으면 회고는 진행되지 않는다.
                    """
            ),
            "알고리즘", List.of(
                    """
                    ## 오늘 풀어본 문제
                    이진 탐색 경계 조건 - lo/hi를 어떻게 잡느냐에 따라 결과가 달라지는 케이스 정리.

                    ## 핵심 정리
                    ```java
                    // 찾는 값의 첫 번째 위치 (lower_bound)
                    int lo = 0, hi = n;
                    while (lo < hi) {
                        int mid = lo + (hi - lo) / 2;
                        if (arr[mid] < target) lo = mid + 1;
                        else hi = mid;
                    }
                    // lo == hi == 정답 인덱스

                    // 찾는 값의 마지막 위치 + 1 (upper_bound)
                    if (arr[mid] <= target) lo = mid + 1;
                    else hi = mid;
                    ```

                    ## 느낀 점
                    `lo < hi`로 종료 조건을 잡고 `hi = mid`로 범위를 좁히는 패턴이 가장 실수가 적다.
                    `lo <= hi`, `hi = mid - 1` 패턴과 섞어 쓰다 틀렸는데, 한 가지 패턴으로 통일해야겠다.
                    """,
                    """
                    ## 오늘 공부한 내용
                    다이나믹 프로그래밍 점화식 세우는 법을 체계적으로 정리했다.

                    ## 핵심 정리
                    1. **상태 정의**: `dp[i]`가 무엇을 의미하는지 먼저 말로 쓰기
                    2. **초기값**: 가장 작은 케이스의 답
                    3. **점화식**: 이전 상태로 현재 상태 표현

                    예시 - 최대 부분합(Kadane's Algorithm):
                    ```java
                    // dp[i] = i번째 원소를 반드시 포함하는 부분 배열의 최대 합
                    dp[i] = Math.max(arr[i], dp[i-1] + arr[i]);
                    answer = Arrays.stream(dp).max().getAsInt();
                    ```

                    ## 느낀 점
                    "상태 정의를 말로 먼저 쓴다"는 습관이 생기니까 점화식이 훨씬 빨리 잡힌다.
                    Top-down(메모이제이션)으로 먼저 구현하고 Bottom-up으로 바꾸는 연습을 해야겠다.
                    """
            ),
            "운동", List.of(
                    """
                    ## 오늘 운동
                    스쿼트 3세트 × 12회 진행. 무릎이 안으로 꺾이는 문제가 있어서 발 너비를 어깨 1.2배로 벌리고 발끝을 30도 바깥으로 했더니 자세가 안정됐다.

                    ## 느낀 점
                    폼부터 잡고 중량을 올리는 게 맞다. 욕심 부리다가 허리 다쳤던 기억이 있어서 이번엔 천천히 가기로 했다.
                    """,
                    """
                    ## 오늘 운동
                    인터벌 트레이닝 — 30초 전력 질주 + 90초 걷기 × 8세트. 심박수 145~165bpm 유지.

                    ## 느낀 점
                    꾸준히 하니 예전보다 심박수가 빨리 안정권으로 돌아온다. 유산소 능력이 분명히 올라가고 있다.
                    """
            )
    );

    // ── 시드 계획 레코드 ──────────────────────────────────────────────────────
    record MonthlyEntry(int monthsAgo, int potIdx, int charCount, String tagKey, int tilCount) {}
    record DailyEntry(int daysAgo, int charCount, String tagKey, int streakDays) {
        DailyEntry(int daysAgo, int charCount, String tagKey) { this(daysAgo, charCount, tagKey, 0); }
    }

    // ── 월별 시드 계획 ────────────────────────────────────────────────────────
    private static final List<MonthlyEntry> MONTHLY_PLAN = List.of(
        // Month -12
        new MonthlyEntry(12, 0, 500, "Java",   6), new MonthlyEntry(12, 0, 600, "Spring", 3),
        new MonthlyEntry(12, 1, 400, "영어",   5), new MonthlyEntry(12, 1, 400, "문법",   3),
        // Month -11
        new MonthlyEntry(11, 0, 520, "Java",   6), new MonthlyEntry(11, 0, 620, "Spring", 4),
        new MonthlyEntry(11, 1, 400, "영어",   5), new MonthlyEntry(11, 1, 400, "문법",   4),
        // Month -10
        new MonthlyEntry(10, 0, 600, "Java",   5), new MonthlyEntry(10, 0, 650, "Spring", 5),
        new MonthlyEntry(10, 1, 420, "영어",   6), new MonthlyEntry(10, 1, 400, "문법",   4),
        // Month -9
        new MonthlyEntry(9, 0, 600, "Java",   5), new MonthlyEntry(9, 0, 600, "Spring", 4),
        new MonthlyEntry(9, 0, 700, "React",  3), new MonthlyEntry(9, 1, 420, "영어",   6),
        new MonthlyEntry(9, 1, 400, "문법",   4),
        // Month -8
        new MonthlyEntry(8, 0, 500, "Java",   3), new MonthlyEntry(8, 0, 600, "Spring", 4),
        new MonthlyEntry(8, 0, 720, "React",  6), new MonthlyEntry(8, 1, 420, "영어",   5),
        new MonthlyEntry(8, 1, 400, "문법",   4),
        // Month -7
        new MonthlyEntry(7, 0, 500, "Java",   2), new MonthlyEntry(7, 0, 550, "Spring", 3),
        new MonthlyEntry(7, 0, 750, "React",  8), new MonthlyEntry(7, 1, 420, "영어",   5),
        new MonthlyEntry(7, 1, 400, "문법",   3),
        // Month -6
        new MonthlyEntry(6, 0, 500, "Java",   2), new MonthlyEntry(6, 0, 720, "React",  8),
        new MonthlyEntry(6, 1, 400, "영어",   5), new MonthlyEntry(6, 1, 380, "문법",   3),
        // Month -5
        new MonthlyEntry(5, 0, 500, "Java",     2), new MonthlyEntry(5, 0, 750, "React",    9),
        new MonthlyEntry(5, 0, 820, "알고리즘", 2), new MonthlyEntry(5, 1, 400, "영어",     4),
        new MonthlyEntry(5, 1, 370, "문법",     3),
        // Month -4
        new MonthlyEntry(4, 0, 500, "Java",     2), new MonthlyEntry(4, 0, 760, "React",    7),
        new MonthlyEntry(4, 0, 870, "알고리즘", 5), new MonthlyEntry(4, 1, 400, "영어",     4),
        new MonthlyEntry(4, 1, 360, "문법",     3),
        // Month -3
        new MonthlyEntry(3, 0, 500, "Java",     2), new MonthlyEntry(3, 0, 720, "React",    5),
        new MonthlyEntry(3, 0, 900, "알고리즘", 7), new MonthlyEntry(3, 1, 400, "영어",     3),
        new MonthlyEntry(3, 1, 360, "문법",     3), new MonthlyEntry(3, 2, 500, "독서",     3),
        // Month -2
        new MonthlyEntry(2, 0, 500, "Java",     2), new MonthlyEntry(2, 0, 720, "React",    5),
        new MonthlyEntry(2, 0, 920, "알고리즘", 8), new MonthlyEntry(2, 1, 400, "영어",     3),
        new MonthlyEntry(2, 1, 350, "문법",     3), new MonthlyEntry(2, 2, 560, "독서",     5),
        // Month -1
        new MonthlyEntry(1, 0, 500, "Java",      2), new MonthlyEntry(1, 0, 720, "React",     5),
        new MonthlyEntry(1, 0, 940, "알고리즘", 10), new MonthlyEntry(1, 1, 400, "영어",      3),
        new MonthlyEntry(1, 1, 350, "문법",      2), new MonthlyEntry(1, 2, 600, "독서",      6),
        // Math pot (potIdx=3)
        new MonthlyEntry(8, 3, 600, "알고리즘", 3), new MonthlyEntry(7, 3, 650, "알고리즘", 3),
        new MonthlyEntry(6, 3, 600, "알고리즘", 2), new MonthlyEntry(5, 3, 700, "알고리즘", 4),
        new MonthlyEntry(4, 3, 650, "알고리즘", 3), new MonthlyEntry(3, 3, 700, "알고리즘", 4),
        new MonthlyEntry(2, 3, 700, "알고리즘", 4), new MonthlyEntry(1, 3, 750, "알고리즘", 5),
        // Fitness pot (potIdx=4)
        new MonthlyEntry(6, 4, 400, "운동", 3), new MonthlyEntry(5, 4, 420, "운동", 3),
        new MonthlyEntry(4, 4, 450, "운동", 2), new MonthlyEntry(3, 4, 500, "운동", 3),
        new MonthlyEntry(2, 4, 450, "운동", 3), new MonthlyEntry(1, 4, 500, "운동", 3)
    );

    // ── 이번 달 일별 시드 계획 ────────────────────────────────────────────────
    private static final List<DailyEntry> CODING_DAYS = List.of(
        new DailyEntry(29, 600, "React"),    new DailyEntry(27, 800, "알고리즘"),
        new DailyEntry(25, 500, "Java"),     new DailyEntry(23, 700, "React"),
        new DailyEntry(21, 400, "Spring"),   new DailyEntry(19, 900, "알고리즘"),
        new DailyEntry(17, 600, "React"),    new DailyEntry(15, 800, "Spring"),
        new DailyEntry(13, 500, "Java"),
        // 연속 14일 스트릭
        new DailyEntry(13, 600, "React",    1),  new DailyEntry(12, 700, "알고리즘", 2),
        new DailyEntry(11, 600, "Java",     3),  new DailyEntry(10, 700, "React",    4),
        new DailyEntry(9,  800, "Spring",   5),  new DailyEntry(8,  500, "알고리즘", 6),
        new DailyEntry(7,  900, "React",    7),  new DailyEntry(6,  600, "Java",     8),
        new DailyEntry(5,  700, "Spring",   9),  new DailyEntry(4,  800, "알고리즘", 10),
        new DailyEntry(3,  650, "React",   11),  new DailyEntry(2,  750, "Java",    12),
        new DailyEntry(1,  600, "Spring",  13),  new DailyEntry(0,  900, "알고리즘", 14)
    );

    private static final List<DailyEntry> ENGLISH_DAYS = List.of(
        new DailyEntry(28, 400, "영어"), new DailyEntry(24, 500, "문법"),
        new DailyEntry(20, 350, "영어"), new DailyEntry(16, 450, "문법"),
        new DailyEntry(12, 500, "영어"), new DailyEntry(8,  400, "문법"),
        new DailyEntry(5,  450, "영어"), new DailyEntry(3,  500, "문법"),
        new DailyEntry(1,  380, "영어"), new DailyEntry(0,  520, "문법")
    );

    private static final List<DailyEntry> READING_DAYS = List.of(
        new DailyEntry(6, 300, "독서"),
        new DailyEntry(3, 350, "독서"),
        new DailyEntry(0, 400, "독서")
    );

    private static final List<DailyEntry> MATH_DAYS = List.of(
        new DailyEntry(22, 600, "알고리즘"), new DailyEntry(15, 700, "알고리즘"),
        new DailyEntry(8,  650, "알고리즘"), new DailyEntry(2,  700, "알고리즘")
    );

    private static final List<DailyEntry> FITNESS_DAYS = List.of(
        new DailyEntry(20, 400, "운동"),
        new DailyEntry(12, 450, "운동"),
        new DailyEntry(5,  400, "운동")
    );

    // ── 제목/본문 인덱스 순환 카운터 ─────────────────────────────────────────
    private final Map<String, Integer> titleIdx = new java.util.HashMap<>();
    private final Map<String, Integer> contentIdx = new java.util.HashMap<>();

    public void seed(UserPotSeeder.SeedContext ctx) {
        User user = ctx.user();
        Pot codingPot  = ctx.codingPot();
        Pot englishPot = ctx.englishPot();
        Pot readingPot = ctx.readingPot();
        Pot mathPot    = ctx.mathPot();
        Pot fitnessPot = ctx.fitnessPot();

        Pot[] pots = {codingPot, englishPot, readingPot, mathPot, fitnessPot};
        LocalDate today = LocalDate.now();
        int[] potExp = {0, 0, 0, 0, 0};

        // ── 태그 초기화 ───────────────────────────────────────────────────
        Map<String, Tag> tags = Map.of(
                "Java",    getOrCreateTag("Java"),
                "Spring",  getOrCreateTag("Spring"),
                "React",   getOrCreateTag("React"),
                "영어",    getOrCreateTag("영어"),
                "문법",    getOrCreateTag("문법"),
                "독서",    getOrCreateTag("독서"),
                "알고리즘", getOrCreateTag("알고리즘"),
                "운동",    getOrCreateTag("운동")
        );

        // ── 월별 데이터 ────────────────────────────────────────────────────
        for (MonthlyEntry row : MONTHLY_PLAN) {
            Tag tag = tags.get(row.tagKey());
            Pot pot = pots[row.potIdx()];
            int exp   = calcExp(row.charCount());
            int point = exp / 10;
            LocalDate base = today.minusMonths(row.monthsAgo()).withDayOfMonth(1);

            for (int i = 0; i < row.tilCount(); i++) {
                LocalDate date = base.plusDays(Math.min(i * 2, 26));
                int before = potExp[row.potIdx()];
                potExp[row.potIdx()] += exp;
                saveTil(user, pot, tag, row.charCount(), exp, point, 0, 1.0,
                        before, potExp[row.potIdx()], date.atTime(21, 0));
            }
        }

        // ── 이번 달 코딩 ──────────────────────────────────────────────────
        int curCodingExp = 0;
        for (DailyEntry d : CODING_DAYS) {
            double mult = 1.0 + Math.min(d.streakDays() * 0.05, 0.5);
            int exp   = (int) Math.floor(calcExp(d.charCount()) * mult);
            int point = exp / 10;
            int before = potExp[0] + curCodingExp;
            curCodingExp += exp;
            saveTil(user, codingPot, tags.get(d.tagKey()), d.charCount(), exp, point,
                    d.streakDays(), mult, before, before + exp,
                    today.minusDays(d.daysAgo()).atTime(21, 0));
        }

        // ── 이번 달 영어 ──────────────────────────────────────────────────
        int curEnglishExp = 0;
        for (DailyEntry d : ENGLISH_DAYS) {
            int exp   = calcExp(d.charCount());
            int point = exp / 10;
            int before = potExp[1] + curEnglishExp;
            curEnglishExp += exp;
            saveTil(user, englishPot, tags.get(d.tagKey()), d.charCount(), exp, point,
                    0, 1.0, before, before + exp,
                    today.minusDays(d.daysAgo()).atTime(20, 0));
        }

        // ── 이번 달 독서 ──────────────────────────────────────────────────
        int curReadingExp = 0;
        for (DailyEntry d : READING_DAYS) {
            int exp   = calcExp(d.charCount());
            int point = exp / 10;
            int before = potExp[2] + curReadingExp;
            curReadingExp += exp;
            saveTil(user, readingPot, tags.get(d.tagKey()), d.charCount(), exp, point,
                    0, 1.0, before, before + exp,
                    today.minusDays(d.daysAgo()).atTime(19, 0));
        }

        // ── 이번 달 수학 ──────────────────────────────────────────────────
        int curMathExp = 0;
        for (DailyEntry d : MATH_DAYS) {
            int exp   = calcExp(d.charCount());
            int point = exp / 10;
            int before = potExp[3] + curMathExp;
            curMathExp += exp;
            saveTil(user, mathPot, tags.get(d.tagKey()), d.charCount(), exp, point,
                    0, 1.0, before, before + exp,
                    today.minusDays(d.daysAgo()).atTime(18, 0));
        }

        // ── 이번 달 운동 ──────────────────────────────────────────────────
        int curFitnessExp = 0;
        for (DailyEntry d : FITNESS_DAYS) {
            int exp   = calcExp(d.charCount());
            int point = exp / 10;
            int before = potExp[4] + curFitnessExp;
            curFitnessExp += exp;
            saveTil(user, fitnessPot, tags.get(d.tagKey()), d.charCount(), exp, point,
                    0, 1.0, before, before + exp,
                    today.minusDays(d.daysAgo()).atTime(17, 0));
        }

        // ── 임시저장 초안 ─────────────────────────────────────────────────
        tilRepository.save(Til.createDraft(user,
                "React Query 캐싱 전략 정리 중",
                "staleTime을 팀 상황에 맞게 조정하는 방법을 정리하다가 멈춤... gcTime이랑 헷갈려서 다시 찾아보는 중",
                codingPot));

        // ── AI 포인트 소비 이력 ───────────────────────────────────────────
        savePointLogWithSign(user, -50, PointLogReason.AI_SUMMARY, today.minusDays(5).atTime(14, 0));
        savePointLogWithSign(user, -30, PointLogReason.AI_QUIZ,    today.minusDays(3).atTime(16, 0));
        savePointLogWithSign(user, -50, PointLogReason.AI_SUMMARY, today.minusDays(1).atTime(11, 0));

        // ── 화분 exp/level 최종 업데이트 ─────────────────────────────────
        int codingTotal  = potExp[0] + curCodingExp;
        int englishTotal = potExp[1] + curEnglishExp;
        int readingTotal = potExp[2] + curReadingExp;
        int mathTotal    = potExp[3] + curMathExp;
        int fitnessTotal = potExp[4] + curFitnessExp;
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                codingTotal,  calcLevel(codingTotal),  codingPot.getId());
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                englishTotal, calcLevel(englishTotal), englishPot.getId());
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                readingTotal, calcLevel(readingTotal), readingPot.getId());
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                mathTotal,    calcLevel(mathTotal),    mathPot.getId());
        jdbcTemplate.update("UPDATE pot SET total_exp=?, level=? WHERE id=?",
                fitnessTotal, calcLevel(fitnessTotal), fitnessPot.getId());

        // ── 유저 최종 포인트 ─────────────────────────────────────────────
        int earned = (codingTotal + englishTotal + readingTotal + mathTotal + fitnessTotal) / 10;
        int used   = 50 + 30 + 50;
        jdbcTemplate.update("UPDATE users SET point=? WHERE id=?",
                Math.max(earned - used, 0), user.getId());

        log.info("TIL 시드 완료 — 포인트: {}P", Math.max(earned - used, 0));
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private void saveTil(User user, Pot pot, Tag tag,
                         int charCount, int exp, int point,
                         int streakDays, double multiplier,
                         int beforeExp, int afterExp,
                         LocalDateTime publishedAt) {
        String title   = nextTitle(tag.getName());
        String content = nextContent(tag.getName());

        Til til = tilRepository.save(Til.create(user, title, content, pot));
        jdbcTemplate.update("UPDATE til SET published_at=? WHERE post_id=?", publishedAt, til.getId());
        tilTagRepository.save(TilTag.of(til, tag));

        int beforeLevel = calcLevel(beforeExp);
        int afterLevel  = calcLevel(afterExp);
        jdbcTemplate.update("""
                INSERT INTO watering_log
                (user_id, pot_id, post_id, exp_gained, point_gained, content_length,
                 streak_days, applied_multiplier, before_pot_level, after_pot_level,
                 before_total_exp, after_total_exp, watered_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                user.getId(), pot.getId(), til.getId(),
                exp, point, charCount,
                streakDays, multiplier, beforeLevel, afterLevel,
                beforeExp, afterExp, publishedAt);

        if (point > 0) {
            PointLog saved = pointLogRepository.save(
                    PointLog.builder().user(user).reason(PointLogReason.TIL_WRITE).amount(point).build());
            jdbcTemplate.update("UPDATE point_log SET created_at=? WHERE id=?", publishedAt, saved.getId());
        }
    }

    private String nextTitle(String tagKey) {
        List<String> pool = switch (tagKey) {
            case "Java"    -> JAVA_TITLES;
            case "Spring"  -> SPRING_TITLES;
            case "React"   -> REACT_TITLES;
            case "영어"    -> ENGLISH_TITLES;
            case "문법"    -> GRAMMAR_TITLES;
            case "독서"    -> READING_TITLES;
            case "알고리즘" -> ALGO_TITLES;
            case "운동"    -> FITNESS_TITLES;
            default        -> List.of(tagKey + " 학습 정리");
        };
        int idx = titleIdx.getOrDefault(tagKey, 0) % pool.size();
        titleIdx.put(tagKey, idx + 1);
        return pool.get(idx);
    }

    private String nextContent(String tagKey) {
        List<String> pool = CONTENTS.getOrDefault(tagKey, List.of("학습 내용 정리"));
        int idx = contentIdx.getOrDefault(tagKey, 0) % pool.size();
        contentIdx.put(tagKey, idx + 1);
        return pool.get(idx).trim();
    }

    private Tag getOrCreateTag(String name) {
        return tagRepository.findByName(name)
                .orElseGet(() -> tagRepository.save(Tag.create(name)));
    }

    private void savePointLogWithSign(User user, int amount, PointLogReason reason, LocalDateTime createdAt) {
        if (amount == 0) return;
        PointLog saved = pointLogRepository.save(
                PointLog.builder().user(user).reason(reason).amount(amount).build());
        jdbcTemplate.update("UPDATE point_log SET created_at=? WHERE id=?", createdAt, saved.getId());
    }

    private static int calcExp(int charCount) {
        return (int) Math.floor(Math.min(charCount * 0.2, 300.0));
    }

    private static int calcLevel(int totalExp) {
        int level = 1, remaining = totalExp;
        while (remaining >= level * 100) { remaining -= level * 100; level++; }
        return level;
    }
}
