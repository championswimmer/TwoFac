<script setup lang="ts">
import { ref } from 'vue'

const WAITLIST_URL =
  'https://script.google.com/macros/s/AKfycbyo_PdD68Cc5R0PatlwicEibQLLRY0NDS4CEgDLoTq-TkqPcodnzQIgUJn9jfeUr8YZKA/exec'

const name = ref('')
const email = ref('')

type State = 'idle' | 'loading' | 'success' | 'already_registered' | 'error'
const state = ref<State>('idle')
const errorMessage = ref('')

async function submit() {
  if (!name.value.trim() || !email.value.trim()) return

  state.value = 'loading'
  errorMessage.value = ''

  try {
    const res = await fetch(WAITLIST_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: JSON.stringify({ email: email.value.trim(), name: name.value.trim() }),
    })

    const data = await res.json()

    if (data.status === 'success') {
      state.value = 'success'
    } else if (data.status === 'already_registered') {
      state.value = 'already_registered'
    } else if (data.error === 'invalid_email_format') {
      state.value = 'error'
      errorMessage.value = 'Please enter a valid email address.'
    } else if (data.error === 'invalid_email_domain') {
      state.value = 'error'
      errorMessage.value = 'Email domain appears to be invalid. Please use a real email address.'
    } else {
      state.value = 'error'
      errorMessage.value = 'Something went wrong. Please try again.'
    }
  } catch {
    state.value = 'error'
    errorMessage.value = 'Network error. Please check your connection and try again.'
  }
}

function reset() {
  state.value = 'idle'
  errorMessage.value = ''
  name.value = ''
  email.value = ''
}
</script>

<template>
  <section class="py-16 sm:py-24 bg-primary-50 dark:bg-primary-950 border-y border-primary-200 dark:border-primary-900">
    <div class="mx-auto max-w-2xl px-4 text-center sm:px-6 lg:px-8">
      <div class="mb-4 inline-flex items-center justify-center h-14 w-14 rounded-2xl bg-primary-100 dark:bg-primary-900 text-primary-600 dark:text-primary-400 text-2xl mx-auto">
        <i class="fa-solid fa-bell" aria-hidden="true" />
      </div>

      <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white sm:text-4xl">
        Join the Waitlist
      </h2>
      <p class="mt-4 text-lg text-secondary-600 dark:text-secondary-400">
        Be the first to know when new platform releases and features drop.
      </p>

      <!-- Success state -->
      <div
        v-if="state === 'success'"
        class="mt-8 rounded-xl border border-green-200 bg-green-50 p-6 dark:border-green-800 dark:bg-green-950"
      >
        <i class="fa-solid fa-circle-check text-3xl text-green-600 dark:text-green-400 mb-3" aria-hidden="true" />
        <p class="text-lg font-semibold text-green-800 dark:text-green-300">You're on the list!</p>
        <p class="mt-1 text-sm text-green-700 dark:text-green-400">
          We'll notify you at <strong>{{ email }}</strong> when there's news.
        </p>
        <button
          class="mt-4 text-sm text-green-700 underline hover:no-underline dark:text-green-400"
          @click="reset"
        >
          Sign up another email
        </button>
      </div>

      <!-- Already registered state -->
      <div
        v-else-if="state === 'already_registered'"
        class="mt-8 rounded-xl border border-yellow-200 bg-yellow-50 p-6 dark:border-yellow-800 dark:bg-yellow-950"
      >
        <i class="fa-solid fa-circle-info text-3xl text-yellow-600 dark:text-yellow-400 mb-3" aria-hidden="true" />
        <p class="text-lg font-semibold text-yellow-800 dark:text-yellow-300">Already registered!</p>
        <p class="mt-1 text-sm text-yellow-700 dark:text-yellow-400">
          <strong>{{ email }}</strong> is already on the waitlist.
        </p>
        <button
          class="mt-4 text-sm text-yellow-700 underline hover:no-underline dark:text-yellow-400"
          @click="reset"
        >
          Try a different email
        </button>
      </div>

      <!-- Form -->
      <form v-else class="mt-8" @submit.prevent="submit">
        <div class="flex flex-col gap-3 sm:flex-row sm:gap-2">
          <input
            v-model="name"
            type="text"
            placeholder="Your name"
            autocomplete="given-name"
            required
            :disabled="state === 'loading'"
            class="flex-1 min-w-0 rounded-lg border border-secondary-300 bg-white px-4 py-3 text-sm text-secondary-900 placeholder-secondary-400 shadow-sm transition focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-60 dark:border-secondary-700 dark:bg-secondary-900 dark:text-white dark:placeholder-secondary-500 dark:focus:border-primary-400 dark:focus:ring-primary-400"
          />
          <input
            v-model="email"
            type="email"
            placeholder="you@example.com"
            autocomplete="email"
            required
            :disabled="state === 'loading'"
            class="flex-1 min-w-0 rounded-lg border border-secondary-300 bg-white px-4 py-3 text-sm text-secondary-900 placeholder-secondary-400 shadow-sm transition focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-60 dark:border-secondary-700 dark:bg-secondary-900 dark:text-white dark:placeholder-secondary-500 dark:focus:border-primary-400 dark:focus:ring-primary-400"
          />
          <button
            type="submit"
            :disabled="state === 'loading'"
            class="inline-flex items-center justify-center gap-2 rounded-lg border border-primary-700 bg-primary-700 px-6 py-3 text-sm font-semibold text-white transition hover:bg-primary-800 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 dark:border-primary-500 dark:bg-primary-600 dark:hover:bg-primary-500"
          >
            <i v-if="state === 'loading'" class="fa-solid fa-spinner animate-spin" aria-hidden="true" />
            <span>{{ state === 'loading' ? 'Joining…' : 'Notify Me' }}</span>
          </button>
        </div>

        <!-- Error message -->
        <p
          v-if="state === 'error'"
          class="mt-3 flex items-center justify-center gap-2 text-sm text-red-600 dark:text-red-400"
        >
          <i class="fa-solid fa-circle-exclamation" aria-hidden="true" />
          {{ errorMessage }}
        </p>
      </form>

      <p class="mt-6 text-xs text-secondary-500 dark:text-secondary-500">
        No spam. Just important release announcements.
      </p>
    </div>
  </section>
</template>
