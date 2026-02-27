package org.sudhir512kj.netflix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "content")
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ContentType type;
    
    @ElementCollection
    @CollectionTable(name = "content_genres")
    private List<String> genres;
    
    @Column(name = "release_year")
    private Integer releaseYear;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "rating")
    private String rating; // PG, PG-13, R, etc.
    
    @Column(name = "imdb_score")
    private Double imdbScore;
    
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    
    @Column(name = "trailer_url")
    private String trailerUrl;
    
    @ElementCollection
    @CollectionTable(name = "content_cast")
    private List<String> cast;
    
    @ElementCollection
    @CollectionTable(name = "content_directors")
    private List<String> directors;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "view_count")
    private Long viewCount = 0L;
    
    // For TV Shows
    @Column(name = "seasons")
    private Integer seasons;
    
    @Column(name = "episodes")
    private Integer episodes;
    
    public enum ContentType {
        MOVIE, TV_SHOW, DOCUMENTARY, ANIME
    }
    
    // Constructors
    public Content() {}
    
    public Content(String title, String description, ContentType type, List<String> genres) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.genres = genres;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public ContentType getType() { return type; }
    public void setType(ContentType type) { this.type = type; }
    
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    
    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    
    public Double getImdbScore() { return imdbScore; }
    public void setImdbScore(Double imdbScore) { this.imdbScore = imdbScore; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    
    public List<String> getCast() { return cast; }
    public void setCast(List<String> cast) { this.cast = cast; }
    
    public List<String> getDirectors() { return directors; }
    public void setDirectors(List<String> directors) { this.directors = directors; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    
    public Integer getSeasons() { return seasons; }
    public void setSeasons(Integer seasons) { this.seasons = seasons; }
    
    public Integer getEpisodes() { return episodes; }
    public void setEpisodes(Integer episodes) { this.episodes = episodes; }
}