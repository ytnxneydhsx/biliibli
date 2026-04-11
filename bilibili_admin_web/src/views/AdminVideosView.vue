<template>
  <section class="page-section">
    <header class="page-header">
      <div>
        <h1>视频管理</h1>
        <p>管理员后台默认首页，后续接入待审核、已通过、已拒绝的真实数据。</p>
      </div>
    </header>

    <div class="tabs-row">
      <button class="tab-button is-active" type="button">待审核</button>
      <button class="tab-button" type="button" disabled>已通过</button>
      <button class="tab-button" type="button" disabled>已拒绝</button>
    </div>

    <div class="video-placeholder-grid">
      <section class="panel">
        <h2>视频列表</h2>
        <p>封面 / 标题 / 投稿人 / 投稿时间 / 时长 / 当前状态 / 操作按钮</p>
      </section>

      <aside class="panel">
        <h2>审核抽屉占位</h2>
        <p>这里后续展示封面、标题、作者、简介和通过/拒绝操作。</p>
      </aside>
    </div>
  </section>
</template>
