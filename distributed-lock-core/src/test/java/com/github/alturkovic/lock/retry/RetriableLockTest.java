/*
 * MIT License
 *
 * Copyright (c) 2020 Alen Turkovic
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.alturkovic.lock.retry;

import com.github.alturkovic.lock.Lock;
import com.github.alturkovic.lock.exception.LockNotAvailableException;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RetriableLockTest {

  @Mock
  private Lock lock;

  @Test
  public void shouldNotRetryWhenFirstAttemptIsSuccessful() {
    when(lock.acquire(anyList(), anyString(), anyLong()))
      .thenReturn("abc");

    final var retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(new NeverRetryPolicy());

    final var retriableLock = new RetriableLock(lock, retryTemplate);
    final var token = retriableLock.acquire(List.of("key"), "defaultStore", 1000L);

    assertThat(token).isEqualTo("abc");
  }

  @Test
  public void shouldRetryWhenFirstAttemptIsNotSuccessful() {
    when(lock.acquire(anyList(), anyString(), anyLong()))
      .thenReturn(null)
      .thenReturn("abc");

    final var retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));

    final var retriableLock = new RetriableLock(lock, retryTemplate);
    final var token = retriableLock.acquire(List.of("key"), "defaultStore", 1000L);

    assertThat(token).isEqualTo("abc");
    verify(lock, times(2)).acquire(anyList(), anyString(), anyLong());
  }

  @Test(expected = LockNotAvailableException.class)
  public void shouldFailRetryWhenFirstAttemptIsNotSuccessful() {
    when(lock.acquire(anyList(), anyString(), anyLong()))
      .thenReturn(null);

    final var retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(new NeverRetryPolicy());

    final var retriableLock = new RetriableLock(lock, retryTemplate);
    final var token = retriableLock.acquire(List.of("key"), "defaultStore", 1000L);

    assertThat(token).isEqualTo("abc");
  }
}