package net.dean.jraw.http;

import com.squareup.okhttp.Response;
import net.dean.jraw.ApiException;
import net.dean.jraw.JrawUtils;
import net.dean.jraw.models.JsonModel;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.RedditObject;
import net.dean.jraw.models.meta.ModelManager;
import org.codehaus.jackson.JsonNode;

/**
 * This class provides automatic parsing of ApiExceptions, as well as quick RedditObject and Listing
 * creation. Note that constructing a RedditResponse will <em>not</em> throw an ApiException. This must be done by the
 * implementer. To see if the response has any errors, use {@link #hasErrors()} and {@link #getErrors()}
 */
public class RedditResponse extends RestResponse {

    private final ApiException[] apiExceptions;

    /**
     * Instantiates a new RedditResponse
     *
     * @param response The Response that will be encapsulated by this object
     */
    public RedditResponse(Response response) {
        super(response);

        ApiException[] errors = new ApiException[0];
        if (JrawUtils.typeComparison(type, MediaTypes.JSON.type()) && !raw.isEmpty()) {
            // Parse the errors into ApiExceptions
            JsonNode errorsNode = rootNode.get("json");
            if (errorsNode != null) {
                errorsNode = errorsNode.get("errors");
            }

            if (errorsNode != null) {
                errors = new ApiException[errorsNode.size()];
                if (errorsNode.size() > 0) {
                    for (int i = 0; i < errorsNode.size(); i++) {
                        JsonNode error = errorsNode.get(i);
                        errors[i] = new ApiException(error.get(0).asText(), error.get(1).asText());
                    }
                }
            }
        }

        this.apiExceptions = errors;
    }

    /** Convenience method to call {@link ModelManager#create(JsonNode, Class)} */
    @SuppressWarnings("unchecked")
    public <T extends JsonModel> T as(Class<T> thingClass) {
        return ModelManager.create(rootNode, thingClass);
    }

    /**
     * This method will return a Listing that represents this JSON response
     *
     * @param thingClass The class of T
     * @param <T> The type of object that the listing will contain
     * @return A new Listing
     */
    public <T extends RedditObject> Listing<T> asListing(Class<T> thingClass) {
        return new Listing<>(rootNode.get("data"), thingClass);
    }

    /**
     * Checks if there were errors returned by the Reddit API
     * @return True if there were errors, false if else
     * @see #getErrors()
     */
    public boolean hasErrors() {
        return apiExceptions.length != 0;
    }

    /**
     * Gets the ApiExceptions returned from the Reddit API
     * @return An array of ApiExceptions
     */
    public ApiException[] getErrors() {
        ApiException[] localCopy = new ApiException[apiExceptions.length];
        for (int i = 0; i < apiExceptions.length; i++) {
            localCopy[i] = new ApiException(apiExceptions[i].getReason(), apiExceptions[i].getExplanation());
        }
        return localCopy;
    }

}
