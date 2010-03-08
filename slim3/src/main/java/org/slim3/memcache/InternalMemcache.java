/*
 * Copyright 2004-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.slim3.memcache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.InvalidValueException;
import com.google.appengine.api.memcache.MemcacheSerialization;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceException;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheServicePb;
import com.google.appengine.api.memcache.Stats;
import com.google.appengine.api.memcache.StrictErrorHandler;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.repackaged.com.google.protobuf.ByteString;
import com.google.appengine.repackaged.com.google.protobuf.InvalidProtocolBufferException;
import com.google.appengine.repackaged.com.google.protobuf.Message;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.CapabilityDisabledException;

/**
 * An internal class to access memcache service.
 * 
 * @author higa
 * @since 3.0
 * 
 */
public class InternalMemcache {

    /**
     * The maximum retry count.
     */
    protected static final int MAX_RETRY = 10;

    /**
     * The initial wait time.
     */
    protected static final long INITIAL_WAIT_MS = 100L;

    /**
     * The multiplier factor for waiting.
     */
    protected static final int WAIT_MULTIPLIER_FACTOR = 2;

    private static final Logger logger =
        Logger.getLogger(InternalMemcache.class.getName());

    /**
     * The memcache service.
     */
    protected MemcacheService ms;

    /**
     * Sleeps.
     * 
     * @param millis
     *            the time to sleep
     */
    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }

    /**
     * Constructor.
     */
    protected InternalMemcache() {
        ms = MemcacheServiceFactory.getMemcacheService();
        ms.setErrorHandler(new StrictErrorHandler());
    }

    /**
     * Empties the cache of all values. Statistics are not affected. Note that
     * clearAll() does not respect namespaces - this flushes the cache for every
     * namespace.
     * 
     * @throws CapabilityDisabledException
     *             if App Engine is read only
     */
    public void cleanAll() throws CapabilityDisabledException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                ms.clearAll();
                return;
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Tests whether a given value is in cache, even if its value is null.
     * 
     * @param key
     *            the key
     * @return whether a given value is in cache
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public boolean contains(Object key) throws IllegalArgumentException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return ms.contains(key);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Removes key from the cache.
     * 
     * @param key
     *            the key
     * @return true if an entry existed to delete
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     * @throws CapabilityDisabledException
     *             if App Engine is read only
     */
    public boolean delete(Object key) throws IllegalArgumentException,
            CapabilityDisabledException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return ms.delete(key);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Removes the given key from the cache, and prevents it from being added
     * under the MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT policy for
     * millisNoReAdd milliseconds thereafter. Calls to a put(java.lang.Object,
     * java.lang.Object, com.google.appengine.api.memcache.Expiration,
     * com.google.appengine.api.memcache.MemcacheService.SetPolicy) method using
     * MemcacheService.SetPolicy.SET_ALWAYS are not blocked, however.
     * 
     * @param key
     *            the key
     * @param millisNoReAdd
     *            the time during which calls to put using ADD_IF_NOT_PRESENT
     *            should be denied
     * @return true if an entry existed to delete
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     * @throws CapabilityDisabledException
     *             if App Engine is read only
     */
    public boolean delete(Object key, long millisNoReAdd)
            throws IllegalArgumentException, CapabilityDisabledException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return ms.delete(key, millisNoReAdd);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Removes keys from the cache.
     * 
     * @param keys
     *            the keys for entries to delete.
     * @return the Set of keys deleted. Any keys in keys but not in the returned
     *         set were not found in the cache.
     * @throws NullPointerException
     *             the keys parameter is null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     * @throws CapabilityDisabledException
     *             if App Engine is read only
     * 
     */
    public Set<Object> deleteAll(Iterable<?> keys) throws NullPointerException,
            IllegalArgumentException, CapabilityDisabledException {
        Collection<Object> keys2 = toCollection(keys);
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return ms.deleteAll(keys2);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Removes keys from the cache.
     * 
     * @param keys
     *            the keys for entries to delete.
     * @param millisNoReAdd
     *            the time during which calls to put using ADD_IF_NOT_PRESENT
     *            should be denied
     * @return the Set of keys deleted. Any keys in keys but not in the returned
     *         set were not found in the cache.
     * @throws NullPointerException
     *             the keys parameter is null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     * @throws CapabilityDisabledException
     *             if App Engine is read only
     * 
     */
    public Set<Object> deleteAll(Iterable<?> keys, long millisNoReAdd)
            throws NullPointerException, IllegalArgumentException,
            CapabilityDisabledException {
        Collection<Object> keys2 = toCollection(keys);
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return ms.deleteAll(keys2, millisNoReAdd);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Returns a previously-stored value, or null if unset. Since null might be
     * the set value in some cases, so we also have contains(Object) which
     * returns boolean.
     * 
     * @param key
     *            the key
     * @return a previously-stored value
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public Object get(Object key) throws IllegalArgumentException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return getInternal(key);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Returns a previously-stored value, or null if unset. Since null might be
     * the set value in some cases, so we also have contains(Object) which
     * returns boolean.
     * 
     * @param key
     *            the key
     * @return a previously-stored value
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    protected Object getInternal(Object key) {
        MemcacheServicePb.MemcacheGetResponse.Builder response =
            MemcacheServicePb.MemcacheGetResponse.newBuilder();
        MemcacheServicePb.MemcacheGetRequest request;
        try {
            request =
                MemcacheServicePb.MemcacheGetRequest
                    .newBuilder()
                    .setNameSpace(ms.getNamespace())
                    .addKey(
                        ByteString.copyFrom(MemcacheSerialization
                            .makePbKey(key)))
                    .build();
        } catch (IOException ex) {
            throw new IllegalArgumentException(
                (new StringBuilder()).append("Cannot use as a key: '").append(
                    key).append("'").toString(),
                ex);
        }
        if (!makeSyncCall("Get", request, response, (new StringBuilder())
            .append("Memcache get: exception getting 1 key (")
            .append(key)
            .append(")")
            .toString()))
            return null;
        if (response.getItemCount() == 0)
            return null;
        MemcacheServicePb.MemcacheGetResponse.Item item = response.getItem(0);
        try {
            return S3MemcacheSerialization.deserialize(item
                .getValue()
                .toByteArray(), item.getFlags());
        } catch (ClassNotFoundException ex) {
            ms.getErrorHandler().handleDeserializationError(
                new InvalidValueException((new StringBuilder()).append(
                    "Can't find class for value of key '").append(key).append(
                    "'").toString(), ex));
        } catch (IOException ex) {
            throw new InvalidValueException((new StringBuilder())
                .append("IO exception parsing value of '")
                .append(key)
                .append("'")
                .toString(), ex);
        }
        return null;
    }

    /**
     * Performs a get of multiple keys at once. This is more efficient than
     * multiple separate calls to get(Object), and allows a single call to both
     * test for contains(Object) and also fetch the value, because the return
     * will not include mappings for keys not found.
     * 
     * @param keys
     *            the keys.
     * @return a mapping from keys to values of any entries found. If a
     *         requested key is not found in the cache, the key will not be in
     *         the returned Map.
     * @throws NullPointerException
     *             if the keys parameter is null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public Map<Object, Object> getAll(Iterable<?> keys)
            throws NullPointerException, IllegalArgumentException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return getAllInternal(keys);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Performs a get of multiple keys at once. This is more efficient than
     * multiple separate calls to get(Object), and allows a single call to both
     * test for contains(Object) and also fetch the value, because the return
     * will not include mappings for keys not found.
     * 
     * @param keys
     *            the keys.
     * @return a mapping from keys to values of any entries found. If a
     *         requested key is not found in the cache, the key will not be in
     *         the returned Map.
     * @throws NullPointerException
     *             if the keys parameter is null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    @SuppressWarnings("unchecked")
    protected Map<Object, Object> getAllInternal(Iterable<?> keys)
            throws NullPointerException, IllegalArgumentException {
        if (keys == null) {
            throw new NullPointerException(
                "The keys parameter must not be null.");
        }
        MemcacheServicePb.MemcacheGetResponse.Builder response =
            MemcacheServicePb.MemcacheGetResponse.newBuilder();
        MemcacheServicePb.MemcacheGetRequest.Builder requestBuilder =
            MemcacheServicePb.MemcacheGetRequest.newBuilder();
        requestBuilder.setNameSpace(ms.getNamespace());
        Map<CacheKey, Object> cacheKeyToObjectKey =
            new HashMap<CacheKey, Object>();
        for (Object key : keys) {
            try {
                byte keybytes[] = MemcacheSerialization.makePbKey(key);
                cacheKeyToObjectKey.put(new CacheKey(keybytes), key);
                requestBuilder.addKey(ByteString.copyFrom(keybytes));
            } catch (IOException ex) {
                throw new IllegalArgumentException((new StringBuilder())
                    .append("Cannot use as key: '")
                    .append(key)
                    .append("'")
                    .toString(), ex);
            }
        }

        if (!makeSyncCall(
            "Get",
            requestBuilder.build(),
            response,
            "Memcache get: exception getting multiple keys"))
            return Collections.emptyMap();
        Map result = new HashMap();
        Iterator i = response.getItemList().iterator();
        do {
            if (!i.hasNext()) {
                break;
            }
            MemcacheServicePb.MemcacheGetResponse.Item item =
                (MemcacheServicePb.MemcacheGetResponse.Item) i.next();
            Object key = null;
            try {
                key =
                    cacheKeyToObjectKey.get(new CacheKey(item
                        .getKey()
                        .toByteArray()));
                Object obj =
                    S3MemcacheSerialization.deserialize(item
                        .getValue()
                        .toByteArray(), item.getFlags());
                result.put(key, obj);
            } catch (ClassNotFoundException ex) {
                ms.getErrorHandler().handleDeserializationError(
                    new InvalidValueException((new StringBuilder())
                        .append("Can't find class for value of key '")
                        .append(key)
                        .append("'")
                        .toString(), ex));
                return null;
            } catch (IOException ex) {
                throw new InvalidValueException((new StringBuilder())
                    .append("IO exception parsing value of '")
                    .append(key)
                    .append("'")
                    .toString(), ex);
            }
        } while (true);
        return result;
    }

    /**
     * Atomically fetches, increments, and stores a given integral value.
     * "Integral" types are Byte, Short, Integer, Long, and in some cases String
     * (if the string is parseable as a number, for example via
     * Long.parseLong(String). The entry must already exist, and have a
     * non-negative value.
     * 
     * Incrementing by positive amounts will reach signed 64-bit max (2^63 - 1)
     * and then wrap-around to signed 64-bit min (-2^63), continuing increments
     * from that point.
     * 
     * To facilitate use as an atomic countdown, incrementing by a negative
     * value (i.e. decrementing) will not go below zero: incrementing 2 by -5
     * will return 0, not -3. However, due to the way numbers are stored,
     * decrementing -3 by -5 will result in -8; so the zero-floor rule only
     * applies to decrementing numbers that were positive.
     * 
     * Note: The actual representation of all numbers in Memcache is a string.
     * This means if you initially stored a number as a string (e.g., "10") and
     * then increment it, everything will work properly, including wrapping
     * beyond signed 64-bit int max. However, if you get the key past the point
     * of wrapping, you will receive an unsigned integer value, not a signed
     * integer value.
     * 
     * @param key
     *            the key
     * @param delta
     *            the size of the increment, positive or negative.
     * @return the post-increment value, as a long. However, a get(Object) of
     *         the key will still have the original type (Byte, Short, etc.). If
     *         there is no entry for key, returns null.
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     * @throws InvalidValueException
     *             if the object incremented is not of a integral type
     */
    public Long increment(Object key, long delta)
            throws IllegalArgumentException, InvalidValueException {
        try {
            return ms.increment(key, delta);
        } catch (MemcacheServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CapabilityDisabledException) {
                throw (CapabilityDisabledException) cause;
            }
            throw e;
        }
    }

    /**
     * Like normal increment, but allows for an optional initial value for the
     * key to take on if not already present in the cache.
     * 
     * @param key
     *            the key
     * @param delta
     *            the size of the increment, positive or negative.
     * @param initialValue
     *            the value to insert into the cache if the key is not present
     * @return the post-increment value, as a long. However, a get(Object) of
     *         the key will still have the original type (Byte, Short, etc.). If
     *         there is no entry for key, returns null.
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     * @throws InvalidValueException
     *             if the object incremented is not of a integral type
     */
    public Long increment(Object key, long delta, long initialValue)
            throws IllegalArgumentException, InvalidValueException {
        try {
            return ms.increment(key, delta, initialValue);
        } catch (MemcacheServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CapabilityDisabledException) {
                throw (CapabilityDisabledException) cause;
            }
            throw e;
        }
    }

    /**
     * Like normal increment, but increments a batch of separate keys in
     * parallel by the same delta.
     * 
     * @param keys
     *            the keys
     * @param delta
     *            the size of the increment, positive or negative.
     * @return mapping keys to their new values; values will be null if they
     *         could not be incremented or were not present in the cache
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public Map<Object, Long> incrementAll(Iterable<?> keys, long delta)
            throws IllegalArgumentException {
        try {
            return ms.incrementAll(toCollection(keys), delta);
        } catch (MemcacheServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CapabilityDisabledException) {
                throw (CapabilityDisabledException) cause;
            }
            throw e;
        }
    }

    /**
     * Like normal increment, but increments a batch of separate keys in
     * parallel by the same delta and potentially sets a starting value.
     * 
     * @param keys
     *            the keys
     * @param delta
     *            the size of the increment, positive or negative.
     * @param initialValue
     *            the value to insert into the cache if the key is not present
     * @return mapping keys to their new values; values will be null if they
     *         could not be incremented for whatever reason
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public Map<Object, Long> incrementAll(Iterable<?> keys, long delta,
            long initialValue) throws IllegalArgumentException {
        try {
            return ms.incrementAll(toCollection(keys), delta, initialValue);
        } catch (MemcacheServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CapabilityDisabledException) {
                throw (CapabilityDisabledException) cause;
            }
            throw e;
        }
    }

    /**
     * Like normal increment, but accepts a mapping of separate controllable
     * offsets for each key individually.
     * 
     * @param offsets
     *            the offsets
     * 
     * @return mapping keys to their new values; values will be null if they
     *         could not be incremented for whatever reason
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public Map<Object, Long> incrementAll(Map<Object, Long> offsets)
            throws IllegalArgumentException {
        try {
            return ms.incrementAll(offsets);
        } catch (MemcacheServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CapabilityDisabledException) {
                throw (CapabilityDisabledException) cause;
            }
            throw e;
        }
    }

    /**
     * Like normal increment, but accepts a mapping of separate controllable
     * offsets for each key individually. Callers may also pass an initial value
     * for the keys to take on if they are not already present in the cache.
     * 
     * @param offsets
     *            the offsets
     * @param initialValue
     *            the value to insert into the cache if the key is not present
     * @return mapping keys to their new values; values will be null if they
     *         could not be incremented for whatever reason
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public Map<Object, Long> incrementAll(Map<Object, Long> offsets,
            long initialValue) throws IllegalArgumentException {
        try {
            return ms.incrementAll(offsets);
        } catch (MemcacheServiceException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CapabilityDisabledException) {
                throw (CapabilityDisabledException) cause;
            }
            throw e;
        }
    }

    /**
     * A convenience shortcut, equivalent to put(key, value, null,
     * SetPolicy.SET_ALWAYS).
     * 
     * @param key
     *            the key
     * @param value
     *            the value
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public void put(Object key, Object value) throws IllegalArgumentException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                ms.put(key, value);
                return;
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * A convenience shortcut, equivalent to put(key, value, expires,
     * SetPolicy.SET_ALWAYS).
     * 
     * @param key
     *            the key
     * @param value
     *            the value
     * @param expires
     *            expiration time for the new values, or null for no time-based
     *            expiration
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public void put(Object key, Object value, Expiration expires)
            throws IllegalArgumentException {
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                ms.put(key, value, expires);
                return;
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Store a new value into the cache, using key, but subject to the policy
     * regarding existing entries.
     * 
     * @param key
     *            the key
     * @param value
     *            the value
     * @param expires
     *            expiration time for the new values, or null for no time-based
     *            expiration
     * @param policy
     *            Requests particular handling regarding pre-existing entries
     *            under the same key. This parameter must not be null.
     * @return true if a new entry was created, false if not because of the
     *         policy.
     * @throws NullPointerException
     *             if the policy parameter is null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public boolean put(Object key, Object value, Expiration expires,
            SetPolicy policy) throws NullPointerException,
            IllegalArgumentException {
        if (policy == null) {
            throw new NullPointerException(
                "The policy parameter must not be null.");
        }
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return ms.put(key, value, expires, policy);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * A convenience shortcut, equivalent to putAll(values, null,
     * SetPolicy.SET_ALWAYS).
     * 
     * @param values
     *            key/value mappings for new entries to add to the cache
     * @throws NullPointerException
     *             if the values parameter is null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public void putAll(Map<Object, Object> values) throws NullPointerException,
            IllegalArgumentException {
        if (values == null) {
            throw new NullPointerException(
                "The values parameter must not be null.");
        }
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                ms.putAll(values);
                return;
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * A convenience shortcut, equivalent to putAll(values, expires,
     * SetPolicy.SET_ALWAYS).
     * 
     * @param values
     *            key/value mappings for new entries to add to the cache
     * @param expires
     *            expiration time for the new values, or null for no time-based
     *            expiration
     * @throws NullPointerException
     *             if the values parameter is null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public void putAll(Map<Object, Object> values, Expiration expires)
            throws NullPointerException, IllegalArgumentException {
        if (values == null) {
            throw new NullPointerException(
                "The values parameter must not be null.");
        }
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                ms.putAll(values, expires);
                return;
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * A batch-processing variant of put(java.lang.Object, java.lang.Object,
     * com.google.appengine.api.memcache.Expiration,
     * com.google.appengine.api.memcache.MemcacheService.SetPolicy). This is
     * more efficiently implemented by the service than multiple calls.
     * 
     * @param values
     *            key/value mappings for new entries to add to the cache
     * @param expires
     *            expiration time for the new values, or null for no time-based
     *            expiration
     * @param policy
     *            what to do if the entry is or is not already present
     * @return the set of keys for which entries were created. Keys in values
     *         may not be in the returned set because of the policy regarding
     *         pre-existing entries.
     * @throws NullPointerException
     *             if the values parameter is null or if the policy parameter is
     *             null
     * @throws IllegalArgumentException
     *             if the key cannot be serialized
     */
    public Set<Object> putAll(Map<Object, Object> values, Expiration expires,
            SetPolicy policy) throws NullPointerException,
            IllegalArgumentException {
        if (values == null) {
            throw new NullPointerException(
                "The values parameter must not be null.");
        }
        if (policy == null) {
            throw new NullPointerException(
                "The policy parameter must not be null.");
        }
        MemcacheServiceException mse = null;
        long wait = INITIAL_WAIT_MS;
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                return ms.putAll(values, expires, policy);
            } catch (MemcacheServiceException e) {
                mse = e;
                Throwable cause = e.getCause();
                if (cause instanceof CapabilityDisabledException) {
                    throw (CapabilityDisabledException) cause;
                }
                logger.log(Level.INFO, "This message["
                    + e
                    + "] is just INFORMATION. Retry["
                    + i
                    + "]", e);
                sleep(wait);
                wait *= WAIT_MULTIPLIER_FACTOR;
            }
        }
        throw mse;
    }

    /**
     * Returns the current error handler.
     * 
     * @return the current error handler.
     */
    public ErrorHandler errorHandler() {
        return ms.getErrorHandler();
    }

    /**
     * Sets the current error handler.
     * 
     * @param errorHandler
     *            the error handler
     * @return this instance
     * @throws NullPointerException
     *             if the errorHandler parameter is null
     */
    public InternalMemcache errorHandler(ErrorHandler errorHandler)
            throws NullPointerException {
        if (errorHandler == null) {
            throw new NullPointerException(
                "The errorHandler parameter must not be null.");
        }
        ms.setErrorHandler(errorHandler);
        return this;
    }

    /**
     * Returns the namespace that will be used in API calls.
     * 
     * @return the namespace.
     */
    public String namespace() {
        return ms.getNamespace();
    }

    /**
     * Sets the current namespace.
     * 
     * @param namespace
     *            the namespace
     * @return this instance
     */
    public InternalMemcache namespace(String namespace) {
        ms.setNamespace(namespace);
        return this;
    }

    /**
     * Returns some statistics about the cache and its usage. Note that
     * statistics() does not respect namespaces - this will return stats for
     * every namespace. The response will never be null.
     * 
     * @return some statistics
     */
    public Stats statistics() {
        return ms.getStatistics();
    }

    /**
     * Makes a sync call.
     * 
     * @param methodName
     *            the method name.
     * @param request
     *            the request.
     * @param response
     *            the response.
     * @param errorText
     *            the error text.
     * @return whether this call succeeded
     */
    protected boolean makeSyncCall(String methodName, Message request,
            Message.Builder response, String errorText) {
        try {
            byte responseBytes[] =
                ApiProxy.makeSyncCall("memcache", methodName, request
                    .toByteArray());
            response.mergeFrom(responseBytes);
            return true;
        } catch (InvalidProtocolBufferException ex) {
            ms.getErrorHandler().handleServiceError(
                new MemcacheServiceException("Could not decode response:", ex));
        } catch (com.google.apphosting.api.ApiProxy.ApplicationException ae) {
            logger.info((new StringBuilder())
                .append(errorText)
                .append(": ")
                .append(ae.getErrorDetail())
                .toString());
            ms.getErrorHandler().handleServiceError(
                new MemcacheServiceException(errorText));
        } catch (com.google.apphosting.api.ApiProxy.ApiProxyException ex) {
            ms.getErrorHandler().handleServiceError(
                new MemcacheServiceException(errorText, ex));
        }
        return false;
    }

    /**
     * Converts the keys to a collection of keys.
     * 
     * @param keys
     *            the keys.
     * @return a collection of the keys.
     * @throws NullPointerException
     *             if the keys parameter is null
     */
    @SuppressWarnings("unchecked")
    protected Collection<Object> toCollection(Iterable<?> keys)
            throws NullPointerException {
        if (keys == null) {
            throw new NullPointerException(
                "The keys parameter must not be null.");
        }
        if (keys instanceof Collection<?>) {
            return (Collection<Object>) keys;
        }
        Collection<Object> ret = new ArrayList<Object>();
        for (Object key : keys) {
            ret.add(key);
        }
        return ret;
    }

    /**
     * A key class for an array of bytes.
     * 
     */
    protected static class CacheKey {

        /**
         * The array of bytes.
         */
        protected byte bytes[];

        /**
         * The cached hash code.
         */
        protected int hashCode;

        /**
         * Constructor.
         * 
         * @param bytes
         *            the array of bytes
         */
        public CacheKey(byte bytes[]) {
            this.bytes = bytes;
            hashCode = Arrays.hashCode(bytes);
        }

        /**
         * Returns the array of bytes.
         * 
         * @return the array of bytes
         */
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof CacheKey) {
                return Arrays.equals(bytes, ((CacheKey) other).bytes);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}