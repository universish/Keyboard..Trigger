Add-Type -AssemblyName System.Windows.Forms
[void][System.Windows.Forms.Application]::EnableVisualStyles()

$form = New-Object System.Windows.Forms.Form
$form.Text = 'Dev: Test Sonucu Gönder'
$form.Size = New-Object System.Drawing.Size(480,360)
$form.StartPosition = 'CenterScreen'

$btn = New-Object System.Windows.Forms.Button
$btn.Text = 'Dev. testi tamamladı'
$btn.Width = 160
$btn.Height = 30
$btn.Top = 10
$btn.Left = 10

$txt = New-Object System.Windows.Forms.TextBox
$txt.Multiline = $true
$txt.ScrollBars = 'Vertical'
$txt.Top = 50
$txt.Left = 10
$txt.Width = 440
$txt.Height = 240

$lbl = New-Object System.Windows.Forms.Label
$lbl.Text = 'Test sonuçlarınızı yazın ve butona bastıktan sonra gönderilecektir.'
$lbl.Top = 300
$lbl.Left = 10
$lbl.Width = 440

$form.Controls.Add($btn)
$form.Controls.Add($txt)
$form.Controls.Add($lbl)

$btn.Add_Click({
    $out = "Time: $(Get-Date -Format o)`n`n" + $txt.Text
    $path = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Definition) 'test_feedback.txt'
    $out | Out-File -FilePath $path -Encoding utf8
    [System.Windows.Forms.MessageBox]::Show('Geri bildirim kaydedildi: ' + $path)
    $form.Close()
})
[void]$form.ShowDialog()
