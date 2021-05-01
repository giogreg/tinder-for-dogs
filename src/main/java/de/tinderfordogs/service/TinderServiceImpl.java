package de.tinderfordogs.service;

import de.tinderfordogs.api.Dog;
import de.tinderfordogs.api.DogRatingRequest;
import de.tinderfordogs.api.RandomDogResponse;
import de.tinderfordogs.persistence.RatedDogEntity;
import de.tinderfordogs.persistence.RatedDogRepository;
import de.tinderfordogs.persistence.Rating;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TinderServiceImpl implements TinderService {

  private static final String RANDOM_DOG_IMAGE_API_URL = "https://dog.ceo/api/breeds/image/random";
  private final RatedDogRepository repository;
  private final RestTemplate restTemplate;
  private final RandomDogNameCreator dogNameCreator;

  public TinderServiceImpl(RatedDogRepository repository, RestTemplate restTemplate, RandomDogNameCreator dogNameCreator) {
    this.repository = repository;
    this.restTemplate = restTemplate;
    this.dogNameCreator = dogNameCreator;
  }

  @Override
  public Dog fetchRandomDog() {
    // TODO: Do error handling, remote api not reachable or no response body etc.
    ResponseEntity<RandomDogResponse> entity = restTemplate.getForEntity(RANDOM_DOG_IMAGE_API_URL, RandomDogResponse.class);
    String name = dogNameCreator.randomDogName();
    String imageUrl = entity.getBody().getMessage();
    RatedDogEntity ratedDogEntity = new RatedDogEntity(name, imageUrl, Rating.UNRATED);
    repository.save(ratedDogEntity);
    return new Dog(ratedDogEntity.getId(), name, imageUrl);
  }

  @Override
  public void likeDog(DogRatingRequest request) {
    RatedDogEntity ratedDogEntity = repository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("Can't find entity for id '" + request.getId() + "'!"));
    ratedDogEntity.setRating(Rating.LIKE);
    repository.save(ratedDogEntity);
  }

  @Override
  public void dislikeDog(DogRatingRequest request) {
    RatedDogEntity ratedDogEntity = repository.findById(request.getId()).orElseThrow(() -> new IllegalArgumentException("Can't find entity for id '" + request.getId() + "'!"));
    ratedDogEntity.setRating(Rating.DISLIKE);
    repository.save(ratedDogEntity);
  }

  @Override
  public List<Dog> fetchAllLikedDogs() {
    List<RatedDogEntity> ratedDogEntities = repository.findAll();
    return ratedDogEntities
      .stream()
      .filter(ratedDog -> ratedDog.getRating() == Rating.LIKE)
      .map(ratedDog -> new Dog(ratedDog.getId(), ratedDog.getName(), ratedDog.getImageUrl()))
      .collect(Collectors.toList());
  }
}
